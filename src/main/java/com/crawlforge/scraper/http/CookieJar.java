package com.crawlforge.scraper.http;

import java.util.List;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;

/** Session/cookie container shared by login flows and subsequent requests. */
public final class CookieJar {

    private final BasicCookieStore store = new BasicCookieStore();

    public void add(Cookie cookie) { store.addCookie(cookie); }
    public void addAll(List<Cookie> cookies) { cookies.forEach(store::addCookie); }
    public List<Cookie> all() { return List.copyOf(store.getCookies()); }
    public int size() { return store.getCookies().size(); }
    public void clear() { store.clear(); }
    BasicCookieStore delegate() { return store; }

    /** Cookie header value for a given host, useful when handing sessions to Selenium/HtmlUnit. */
    public String cookieHeaderFor(String host) {
        StringBuilder header = new StringBuilder();
        String hostname = host.toLowerCase(java.util.Locale.ROOT);
        for (Cookie cookie : store.getCookies()) {
            String domain = cookie.getDomain() == null ? "" : cookie.getDomain()
                    .replaceFirst("^\\.", "").toLowerCase(java.util.Locale.ROOT);
            boolean matches = domain.isEmpty() || hostname.equals(domain) || hostname.endsWith("." + domain);
            if (matches) {
                if (header.length() > 0) header.append("; ");
                header.append(cookie.getName()).append('=').append(cookie.getValue());
            }
        }
        return header.toString();
    }
}
