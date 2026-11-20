package com.crawlforge.scraper.crawler;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.parsers.ParserRegistry;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Resolves next-page and follow links, keeps the crawl on-domain and removes duplicates. */
public final class PaginationHandler {

    private final ParserRegistry parserRegistry;

    public PaginationHandler(ParserRegistry parserRegistry) { this.parserRegistry = parserRegistry; }

    public List<String> discover(String html, SiteConfig site, String pageUrl) {
        List<String> resolved = new ArrayList<>();
        for (String rawLink : parserRegistry.links(html, site)) {
            String absolute = absolutize(pageUrl, rawLink);
            if (absolute != null && allowed(absolute, site) && !resolved.contains(absolute)) {
                resolved.add(absolute);
            }
        }
        return List.copyOf(resolved);
    }

    public String absolutize(String base, String href) {
        try {
            if (href == null || href.isBlank()) return null;
            return URI.create(base).resolve(href).normalize().toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean allowed(String url, SiteConfig site) {
        if (site.baseUrl() == null) return true;
        try {
            String target = URI.create(url).getHost();
            String base = URI.create(site.baseUrl()).getHost();
            return target != null && target.equalsIgnoreCase(base);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
