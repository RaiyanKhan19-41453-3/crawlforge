package com.crawlforge.scraper.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;

/**
 * Rotates HTTP proxies to spread requests across egress IPs.
 * Supported formats: host:port or user:password@host:port.
 */
public final class ProxyRotator {

    private final List<HttpHost> proxies;
    private final Map<HttpHost, UsernamePasswordCredentials> credentials;
    private final AtomicInteger cursor = new AtomicInteger();

    public ProxyRotator(List<String> definitions) {
        List<HttpHost> hosts = new java.util.ArrayList<>();
        Map<HttpHost, UsernamePasswordCredentials> creds = new LinkedHashMap<>();
        if (definitions != null) {
            for (String definition : definitions) {
                if (definition == null || definition.isBlank()) continue;
                String value = definition.trim();
                int at = value.lastIndexOf('@');
                String credentialPart = at > 0 ? value.substring(0, at) : null;
                String hostPort = at > 0 ? value.substring(at + 1) : value;
                String[] parts = hostPort.split(":");
                if (parts.length != 2) continue;
                try {
                    HttpHost host = new HttpHost(parts[0], Integer.parseInt(parts[1]));
                    hosts.add(host);
                    if (credentialPart != null) {
                        String[] userPass = credentialPart.split(":", 2);
                        if (userPass.length == 2) {
                            creds.put(host, new UsernamePasswordCredentials(userPass[0], userPass[1].toCharArray()));
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // Skip malformed definitions instead of failing the whole crawler.
                }
            }
        }
        this.proxies = List.copyOf(hosts);
        this.credentials = Map.copyOf(creds);
    }

    /** Returns null when no proxy is configured, meaning a direct connection. */
    public HttpHost next() {
        if (proxies.isEmpty()) return null;
        int index = Math.floorMod(cursor.getAndIncrement(), proxies.size());
        return proxies.get(index);
    }

    public boolean hasAny() { return !proxies.isEmpty(); }
    public int size() { return proxies.size(); }

    /** Registers credentials for authenticated proxies (never logs them). */
    public void applyCredentialsTo(BasicCredentialsProvider provider) {
        credentials.forEach((host, value) -> provider.setCredentials(new AuthScope(host), value));
    }
}
