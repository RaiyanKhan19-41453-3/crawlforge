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
        for (String rawLink : parserRegistry.links(html, site, pageUrl)) {
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
            URI target = URI.create(url);
            URI base = URI.create(site.baseUrl());
            if (target.getHost() == null || base.getHost() == null) return false;
            if (!target.getHost().equalsIgnoreCase(base.getHost())) return false;
            String targetScheme = target.getScheme() == null ? "" : target.getScheme().toLowerCase(java.util.Locale.ROOT);
            String baseScheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(java.util.Locale.ROOT);
            // Same scheme, or a safe http -> https upgrade on the same host.
            boolean schemeOk = targetScheme.equals(baseScheme)
                    || (baseScheme.equals("http") && targetScheme.equals("https"));
            if (!schemeOk) return false;
            // Different explicit ports on the same host are a different service.
            return target.getPort() <= 0 || base.getPort() <= 0 || target.getPort() == base.getPort();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
