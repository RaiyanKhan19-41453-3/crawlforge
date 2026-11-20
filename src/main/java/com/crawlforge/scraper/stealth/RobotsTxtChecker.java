package com.crawlforge.scraper.stealth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal robots.txt check for the common {@code User-agent: * / Disallow: /path} rules.
 * Uses the JDK HTTP client so it never re-enters the crawler's own HTTP pipeline.
 */
public final class RobotsTxtChecker {

    public static final RobotsTxtChecker INSTANCE = new RobotsTxtChecker();

    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    private RobotsTxtChecker() { }

    public boolean allowed(String url) {
        try {
            URI uri = URI.create(url);
            String origin = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            Set<String> disallowed = cache.computeIfAbsent(origin, this::load);
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            return disallowed.stream().noneMatch(path::startsWith);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private Set<String> load(String origin) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(origin + "/robots.txt"))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return Set.of();

            Set<String> rules = new HashSet<>();
            boolean wildcardGroup = false;
            for (String rawLine : response.body().split("\\R")) {
                String line = rawLine.replace("#", "").trim();
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if (key.equalsIgnoreCase("User-agent")) {
                    wildcardGroup = "*".equals(value);
                } else if (key.equalsIgnoreCase("Disallow") && wildcardGroup && !value.isEmpty()) {
                    rules.add(value);
                }
            }
            return Set.copyOf(rules);
        } catch (Exception e) {
            return Set.of(); // No robots.txt available means crawling is allowed.
        }
    }

    /** Clears cached rules, useful in tests or after a long-running job. */
    public void clearCache() { cache.clear(); }
}
