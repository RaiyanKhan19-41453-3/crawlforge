package com.crawlforge.scraper.http;

import java.util.Map;

/** Small transport-independent response object returned by the HTTP layer. */
public record HttpResponse(int status, String body, String url, Map<String, String> headers) {

    public boolean isSuccess() { return status >= 200 && status < 300; }

    public String header(String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }
}
