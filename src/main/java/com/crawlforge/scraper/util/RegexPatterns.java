package com.crawlforge.scraper.util;

import java.util.regex.Pattern;

/** Reusable regular expressions used by {@link RegexUtils}, normalisers and validators. */
public final class RegexPatterns {

    private RegexPatterns() {}

    public static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    public static final Pattern PHONE = Pattern.compile(
            "(?:(?:\\+?88)[\\s.-]?)?0?1[3-9](?:[\\s.-]?\\d){8}\\b");
    public static final Pattern URL = Pattern.compile(
            "https?://[\\w.-]+(?::\\d+)?(?:/[\\w~./?%&=+#-]*)?");
    public static final Pattern PRICE = Pattern.compile(
            "\\b[\\d]{1,3}(?:,\\d{2,3})*(?:\\.\\d{2})?\\b");
    public static final Pattern DATE_ISO = Pattern.compile(
            "\\b\\d{4}-\\d{2}-\\d{2}(?:[T ]\\d{2}:\\d{2}(?::\\d{2})?)?\\b");
    public static final Pattern CARD_PAN = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    public static final Pattern SKU = Pattern.compile("\\b[A-Z]{2,5}-?\\d{3,8}\\b");
    public static final Pattern WHITESPACE = Pattern.compile("\\s+");
}
