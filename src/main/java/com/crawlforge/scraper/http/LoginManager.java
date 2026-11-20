package com.crawlforge.scraper.http;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.FetchException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Performs form logins and keeps the resulting session in the shared cookie jar. */
public final class LoginManager {

    private static final Logger log = LoggerFactory.getLogger(LoginManager.class);
    private final ResilientHttpClient http;

    public LoginManager(ResilientHttpClient http) { this.http = http; }

    /** Returns true when a login was required and succeeded. */
    public boolean loginIfNeeded(SiteConfig site) {
        if (!site.requiresLogin()) return false;
        log.info("Authenticating with site '{}' as user '{}'", site.name(), site.username());
        Map<String, String> form = new LinkedHashMap<>();
        form.put("username", site.username());
        form.put("password", site.password());
        HttpResponse response = http.postForm(site.loginUrl(), form);
        if (!response.isSuccess()) {
            throw new FetchException("Login failed for " + site.name() + ": HTTP " + response.status());
        }
        if (http.cookies().size() == 0) {
            throw new FetchException("Login returned no session cookie for " + site.name());
        }
        log.info("Login successful for '{}' (session cookies: {})", site.name(), http.cookies().size());
        return true;
    }

    /** Cookie header that can be handed to Selenium / HtmlUnit for the same session. */
    public String sessionCookieHeader(String host) {
        return http.cookies().cookieHeaderFor(host);
    }
}
