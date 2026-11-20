package com.crawlforge.scraper.core.config;

import com.crawlforge.scraper.core.model.SourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Declarative scrape recipe. Selectors live here, not in Java code, so a site change is a
 * configuration edit (JIRA ticket + PR) instead of a code release.
 */
public final class SiteConfig {

    private final String name;
    private final SourceType sourceType;
    private final List<String> seedUrls;
    private final String baseUrl;
    private final String loginUrl;
    private final String username;
    private final String password;
    private final String itemSelector;
    private final String nextPageSelector;
    private final String waitForSelector;
    private final Map<String, String> fields;
    private final Map<String, String> xpathFields;
    private final String jsonResultPath;
    private final String jsonIdField;
    private final int maxPages;
    private final int maxDepth;
    private final boolean followLinks;
    private final boolean javascriptRequired;
    private final boolean csvHasHeader;

    private SiteConfig(Builder b) {
        this.name = Objects.requireNonNullElse(b.name, "unnamed-site");
        this.sourceType = Objects.requireNonNullElse(b.sourceType, SourceType.HTML);
        this.seedUrls = List.copyOf(b.seedUrls);
        this.baseUrl = b.baseUrl;
        this.loginUrl = b.loginUrl;
        this.username = b.username;
        this.password = b.password;
        this.itemSelector = b.itemSelector;
        this.nextPageSelector = b.nextPageSelector;
        this.waitForSelector = b.waitForSelector;
        this.fields = Map.copyOf(b.fields);
        this.xpathFields = Map.copyOf(b.xpathFields);
        this.jsonResultPath = b.jsonResultPath;
        this.jsonIdField = b.jsonIdField;
        this.maxPages = b.maxPages;
        this.maxDepth = b.maxDepth;
        this.followLinks = b.followLinks;
        this.javascriptRequired = b.javascriptRequired;
        this.csvHasHeader = b.csvHasHeader;
    }

    public static Builder builder() { return new Builder(); }

    public String name() { return name; }
    public SourceType sourceType() { return sourceType; }
    public List<String> seedUrls() { return seedUrls; }
    public String baseUrl() { return baseUrl; }
    public String loginUrl() { return loginUrl; }
    public String username() { return username; }
    public String password() { return password; }
    public String itemSelector() { return itemSelector; }
    public String nextPageSelector() { return nextPageSelector; }
    public String waitForSelector() { return waitForSelector; }
    public Map<String, String> fields() { return fields; }
    public Map<String, String> xpathFields() { return xpathFields; }
    public String jsonResultPath() { return jsonResultPath; }
    public String jsonIdField() { return jsonIdField; }
    public int maxPages() { return maxPages; }
    public int maxDepth() { return maxDepth; }
    public boolean followLinks() { return followLinks; }
    public boolean javascriptRequired() { return javascriptRequired; }
    public boolean csvHasHeader() { return csvHasHeader; }
    public boolean requiresLogin() { return loginUrl != null && username != null; }

    @Override
    public String toString() { return "SiteConfig{" + name + ", " + sourceType + ", seeds=" + seedUrls.size() + '}'; }

    public static final class Builder {
        private String name;
        private SourceType sourceType = SourceType.HTML;
        private final List<String> seedUrls = new ArrayList<>();
        private String baseUrl;
        private String loginUrl;
        private String username;
        private String password;
        private String itemSelector;
        private String nextPageSelector;
        private String waitForSelector;
        private final Map<String, String> fields = new LinkedHashMap<>();
        private final Map<String, String> xpathFields = new LinkedHashMap<>();
        private String jsonResultPath = "$";
        private String jsonIdField = "id";
        private int maxPages = 50;
        private int maxDepth = 3;
        private boolean followLinks = true;
        private boolean javascriptRequired;
        private boolean csvHasHeader = true;

        public Builder name(String name) { this.name = name; return this; }
        public Builder sourceType(SourceType t) { this.sourceType = t; return this; }
        public Builder seedUrl(String url) { if (url != null && !url.isBlank()) seedUrls.add(url); return this; }
        public Builder seedUrls(List<String> urls) { if (urls != null) urls.forEach(this::seedUrl); return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder loginUrl(String loginUrl) { this.loginUrl = loginUrl; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder itemSelector(String s) { this.itemSelector = s; return this; }
        public Builder nextPageSelector(String s) { this.nextPageSelector = s; return this; }
        public Builder waitForSelector(String s) { this.waitForSelector = s; return this; }
        public Builder field(String name, String selector) { fields.put(name, selector); return this; }
        public Builder xpathField(String name, String xpath) { xpathFields.put(name, xpath); return this; }
        public Builder jsonResultPath(String p) { this.jsonResultPath = p; return this; }
        public Builder jsonIdField(String f) { this.jsonIdField = f; return this; }
        public Builder maxPages(int maxPages) { this.maxPages = maxPages; return this; }
        public Builder maxDepth(int maxDepth) { this.maxDepth = maxDepth; return this; }
        public Builder followLinks(boolean follow) { this.followLinks = follow; return this; }
        public Builder javascriptRequired(boolean js) { this.javascriptRequired = js; return this; }
        public Builder csvHasHeader(boolean value) { this.csvHasHeader = value; return this; }
        public SiteConfig build() { return new SiteConfig(this); }
    }
}
