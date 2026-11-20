package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.util.RegexPatterns;
import java.text.Normalizer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/** Cleans raw extracted text so the client receives consistent, analysis-ready data. */
public final class DataNormalizer {

    private DataNormalizer() {}

    public static String normalize(String value) {
        if (value == null) return "";
        String cleaned = Normalizer.normalize(value, Normalizer.Form.NFKC);
        cleaned = cleaned.replace('\u00a0', ' ');
        cleaned = RegexPatterns.WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        return cleaned;
    }

    public static String stripHtml(String value) {
        if (value == null) return "";
        return normalize(Jsoup.clean(value, Safelist.none()));
    }

    public static String truncate(String value, int maxLength) {
        String safe = normalize(value);
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength - 1) + "…";
    }

    public static String numeric(String value) {
        String normalized = normalize(value).replace(",", "");
        int index = normalized.indexOf('.');
        if (index > 0 && normalized.length() - index > 3) {
            return normalized.substring(0, index + 3);
        }
        return normalized;
    }
}
