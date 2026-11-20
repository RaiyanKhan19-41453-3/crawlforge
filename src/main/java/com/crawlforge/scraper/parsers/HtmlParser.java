package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/** Static HTML extraction with CSS selectors, XPath support and link discovery. */
public final class HtmlParser implements Parser {

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.HTML || type == SourceType.DYNAMIC_HTML;
    }

    @Override
    public List<ScrapedRecord> parse(String raw, SiteConfig site) {
        try {
            Document doc = Jsoup.parse(raw, site.baseUrl() == null ? "" : site.baseUrl());
            Elements items = site.itemSelector() == null ? doc.select("body") : doc.select(site.itemSelector());
            List<ScrapedRecord> results = new ArrayList<>();
            for (Element item : items) {
                Map<String, String> attributes = new LinkedHashMap<>();
                site.fields().forEach((name, selector) -> attributes.put(name, extract(item, selector)));
                site.xpathFields().forEach((name, xpath) -> {
                    List<String> values = XPathSelector.select(item, xpath);
                    attributes.put(name, values.isEmpty() ? "" : values.get(0));
                });
                attributes.replaceAll((key, value) -> DataNormalizer.normalize(value));

                String url = item.attr("abs:href");
                String mappedUrl = attributes.get("url");
                if (mappedUrl != null && !mappedUrl.isBlank()) {
                    url = absoluteUrl(mappedUrl, site.baseUrl());
                }
                if (url.isBlank() && site.baseUrl() != null) url = site.baseUrl();
                String title = attributes.getOrDefault("title", firstText(item, "h1,h2,h3,.title"));
                String content = attributes.getOrDefault("content", readableText(item));
                results.add(ScrapedRecord.builder()
                        .source(site.name())
                        .url(url)
                        .title(DataNormalizer.normalize(title))
                        .content(DataNormalizer.truncate(content, 10_000))
                        .attributes(attributes)
                        .qualityScore(DataValidator.score(url, content, attributes))
                        .build());
            }
            return List.copyOf(results);
        } catch (RuntimeException e) {
            throw new ParseException("Could not parse HTML for site " + site.name(), e);
        }
    }

    @Override
    public List<String> links(String raw, SiteConfig site) {
        return links(raw, site, site.baseUrl());
    }

    @Override
    public List<String> links(String raw, SiteConfig site, String pageUrl) {
        String base = pageUrl == null || pageUrl.isBlank() ? site.baseUrl() : pageUrl;
        Document doc = Jsoup.parse(raw, base == null ? "" : base);
        List<String> links = new ArrayList<>();
        if (site.nextPageSelector() != null && !site.nextPageSelector().isBlank()) {
            Element next = doc.selectFirst(site.nextPageSelector());
            if (next != null) addIfUseful(links, next.attr("abs:href"));
        }
        if (site.followLinks()) {
            for (Element anchor : doc.select("a[href]")) {
                if (links.size() >= 40) break;
                addIfUseful(links, anchor.attr("abs:href"));
            }
        }
        return List.copyOf(links);
    }

    private static void addIfUseful(List<String> links, String url) {
        if (url == null || !url.startsWith("http")) return;
        int fragment = url.indexOf('#');
        if (fragment >= 0) url = url.substring(0, fragment);
        if (!url.isBlank() && !links.contains(url)) links.add(url);
    }

    private static String extract(Element item, String selector) {
        if (selector == null) return "";
        if (selector.startsWith("attr:")) return item.attr(selector.substring(5));
        Element found = item.selectFirst(selector);
        return found == null ? "" : found.text();
    }

    private static String absoluteUrl(String value, String base) {
        if (value.startsWith("http")) return value;
        if (base == null || base.isBlank()) return value;
        try {
            return java.net.URI.create(base).resolve(value).toString();
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    /** Jsoup's Element.text() concatenates inline siblings without separators; keep them readable. */
    private static String readableText(Element item) {
        StringBuilder text = new StringBuilder();
        for (Node child : item.childNodes()) {
            String value = child instanceof Element element ? element.text()
                    : child instanceof TextNode node ? node.getWholeText() : "";
            value = DataNormalizer.normalize(value);
            if (!value.isEmpty()) {
                if (text.length() > 0) text.append(' ');
                text.append(value);
            }
        }
        return text.toString();
    }

    private static String firstText(Element item, String selector) {
        Element found = item.selectFirst(selector);
        return found == null ? "" : found.text();
    }
}
