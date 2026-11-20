package com.crawlforge.scraper.core.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable data carrier for one extracted item. Every record carries a quality score so
 * downstream data-quality checks can reject bad rows instead of silently corrupting a client feed.
 */
public final class ScrapedRecord {

    private final String source;
    private final String url;
    private final String title;
    private final String content;
    private final Map<String, String> attributes;
    private final double qualityScore;
    private final Instant scrapedAt;

    private ScrapedRecord(Builder builder) {
        this.source = builder.source;
        this.url = builder.url;
        this.title = builder.title;
        this.content = builder.content;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
        this.qualityScore = builder.qualityScore;
        this.scrapedAt = builder.scrapedAt == null ? Instant.now() : builder.scrapedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String source() { return source; }
    public String url() { return url; }
    public String title() { return title; }
    public String content() { return content; }
    public Map<String, String> attributes() { return attributes; }
    public double qualityScore() { return qualityScore; }
    public Instant scrapedAt() { return scrapedAt; }

    /** Cheap structural validation used before a record is persisted or exported. */
    public boolean isValid() {
        boolean hasPayload = (content != null && !content.isBlank()) || !attributes.isEmpty();
        return url != null && url.startsWith("http") && hasPayload && qualityScore >= 0.0;
    }

    /** Flat representation used by CSV/Excel/JSON writers and the SQL repository. */
    public Map<String, String> toMap() {
        Map<String, String> flat = new HashMap<>(attributes);
        flat.put("source", Objects.toString(source, ""));
        flat.put("url", Objects.toString(url, ""));
        flat.put("title", Objects.toString(title, ""));
        flat.put("content", Objects.toString(content, ""));
        flat.put("quality_score", String.format(java.util.Locale.ROOT, "%.3f", qualityScore));
        flat.put("scraped_at", scrapedAt.toString());
        return flat;
    }

    /** Copy with a corrected source URL - used when the parser only knew the site root. */
    public ScrapedRecord withUrl(String newUrl) {
        return builder()
                .source(source)
                .url(newUrl)
                .title(title)
                .content(content)
                .attributes(attributes)
                .qualityScore(qualityScore)
                .scrapedAt(scrapedAt)
                .build();
    }

    @Override
    public String toString() {
        return "ScrapedRecord{source='" + source + "', url='" + url + "', title='" + title
                + "', fields=" + attributes.size() + ", quality=" + qualityScore + '}';
    }

    public static final class Builder {
        private String source;
        private String url;
        private String title;
        private String content;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private double qualityScore = 1.0;
        private Instant scrapedAt;

        public Builder source(String source) { this.source = source; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder attribute(String key, String value) {
            if (key != null && value != null) attributes.put(key, value);
            return this;
        }
        public Builder attributes(Map<String, String> values) {
            if (values != null) values.forEach(this::attribute);
            return this;
        }
        public Builder qualityScore(double qualityScore) { this.qualityScore = qualityScore; return this; }
        public Builder scrapedAt(Instant scrapedAt) { this.scrapedAt = scrapedAt; return this; }
        public ScrapedRecord build() { return new ScrapedRecord(this); }
    }
}
