package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.util.List;
import java.util.Map;

/** Data-quality gate: scores each record and filters rows that cannot be trusted. */
public final class DataValidator {

    public static final double MIN_ACCEPTABLE_SCORE = 0.4;

    private DataValidator() {}

    /** Score in [0, 1]. Low scores mean missing/empty fields that must not reach the client. */
    public static double score(String url, String content, Map<String, String> attributes) {
        double score = 1.0;
        if (url == null || !(url.startsWith("http") || url.startsWith("csv://") || url.startsWith("pdf://"))) {
            score -= 0.4;
        }
        if (content == null || content.isBlank()) score -= 0.3;
        else if (content.length() < 8) score -= 0.1;
        if (attributes == null || attributes.isEmpty()) score -= 0.3;
        else {
            long empty = attributes.values().stream().filter(v -> v == null || v.isBlank()).count();
            score -= 0.15 * empty;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    public static boolean acceptable(ScrapedRecord record) {
        return record.isValid() && record.qualityScore() >= MIN_ACCEPTABLE_SCORE;
    }

    public static List<ScrapedRecord> filter(List<ScrapedRecord> records) {
        return records.stream().filter(DataValidator::acceptable).toList();
    }
}
