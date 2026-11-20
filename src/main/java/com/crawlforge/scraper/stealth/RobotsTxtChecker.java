package com.crawlforge.scraper.stealth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpClient.Redirect;
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
    // RFC 9309: 401/403 on robots.txt means complete disallow. "" matches every path.
    private static final Set<String> DISALLOW_ALL = Set.of("");

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
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(3)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403
                    || response.statusCode() >= 500) return DISALLOW_ALL;
            if (response.statusCode() >= 400) return Set.of(); // 404 means no rules

            Set<String> rules = new HashSet<>();
            boolean inAgentBlock = false;
            boolean groupAppliesToWildcard = false;
            for (String rawLine : response.body().split("\\R")) {
                int comment = rawLine.indexOf('#');
                String line = (comment >= 0 ? rawLine.substring(0, comment) : rawLine).trim();
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if (key.equalsIgnoreCase("User-agent")) {
                    if (!inAgentBlock) {
                        inAgentBlock = true;
                        groupAppliesToWildcard = false;
                    }
                    if ("*".equals(value)) groupAppliesToWildcard = true;
                } else if (key.equalsIgnoreCase("Disallow")) {
                    inAgentBlock = false;
                    if (groupAppliesToWildcard && !value.isEmpty()) rules.add(value);
                } else {
                    inAgentBlock = false;
                }
            }
            return Set.copyOf(rules);
        } catch (Exception e) {
            // RFC 9309: if robots.txt is unreachable, treat the site as fully disallowed.
            return DISALLOW_ALL;
        }
    }

    /** Clears cached rules, useful in tests or after a long-running job. */
    public void clearCache() { cache.clear(); }
}
