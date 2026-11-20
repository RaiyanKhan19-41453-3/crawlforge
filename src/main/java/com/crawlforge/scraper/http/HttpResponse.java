package com.crawlforge.scraper.http;

import java.util.Map;

/** Transport-independent response object returned by the HTTP layer. */
public record HttpResponse(int status, String body, String url, String finalUrl, Map<String, String> headers) {

    public boolean isSuccess() { return status >= 200 && status < 300; }

    /** URL after any redirects, used as the base for relative links and record provenance. */
    public String effectiveUrl() { return finalUrl == null ? url : finalUrl; }

    public String header(String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }
}
