package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** XML extraction using secure DOM parsing and XPath expressions. */
public final class XmlParser implements Parser {

    @Override
    public boolean supports(SourceType type) { return type == SourceType.XML; }

    @Override
    public List<ScrapedRecord> parse(String raw, SiteConfig site) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            // Namespace prefixes are stripped so recipes can use simple XPath names such as
            // //item even when the client feed uses <x:item>.
            String normalized = raw.replaceAll("\\s+xmlns(?::[\\w.-]+)?\\s*=\\s*(['\"])[^'\"]*\\1", "")
                    .replaceAll("<(/?)(\\w+):", "<$1");
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(normalized)));
            String itemExpression = site.itemSelector() == null ? "//item" : site.itemSelector();
            NodeList items = (NodeList) XPathFactory.newInstance().newXPath()
                    .evaluate(itemExpression, doc, XPathConstants.NODESET);

            List<ScrapedRecord> results = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                Map<String, String> attributes = new LinkedHashMap<>();
                site.fields().forEach((name, expression) -> {
                    List<String> values = XPathSelector.select(item, expression);
                    attributes.put(name, values.isEmpty() ? "" : values.get(0));
                });
                site.xpathFields().forEach((name, expression) -> {
                    List<String> values = XPathSelector.select(item, expression);
                    attributes.put(name, values.isEmpty() ? "" : values.get(0));
                });
                attributes.replaceAll((key, value) -> DataNormalizer.normalize(value));

                String url = attributes.getOrDefault("url", site.baseUrl());
                String title = attributes.getOrDefault("title", "");
                String content = attributes.getOrDefault("content",
                        DataNormalizer.normalize(item.getTextContent()));
                results.add(ScrapedRecord.builder()
                        .source(site.name())
                        .url(url)
                        .title(title)
                        .content(DataNormalizer.truncate(content, 10_000))
                        .attributes(attributes)
                        .qualityScore(DataValidator.score(url, content, attributes))
                        .build());
            }
            return List.copyOf(results);
        } catch (Exception e) {
            throw new ParseException("Could not parse XML for site " + site.name(), e);
        }
    }
}
