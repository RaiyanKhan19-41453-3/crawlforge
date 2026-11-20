package com.crawlforge.scraper.payment;

import com.crawlforge.scraper.util.RegexUtils;

/** Masking and brand detection helpers: PANs never leave the process unmasked. */
public final class CardDataMasker {

    private CardDataMasker() {}

    public static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    public static String mask(String pan) {
        String digits = digits(pan);
        if (digits.length() <= 4) return "****";
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }

    public static String brand(String pan) {
        String digits = digits(pan);
        if (digits.startsWith("4")) return "VISA";
        if (digits.matches("(?:5[1-5]|2[2-7])[0-9]{14,17}")) return "MASTERCARD";
        if (digits.matches("3[47][0-9]{13}")) return "AMEX";
        return "UNKNOWN";
    }

    public static boolean valid(String pan) { return RegexUtils.passesLuhn(pan); }
}
