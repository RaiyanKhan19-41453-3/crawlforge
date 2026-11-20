package com.crawlforge.scraper.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helpers built on {@link RegexPatterns} so complex regex is used consistently. */
public final class RegexUtils {

    private RegexUtils() {}

    public static Optional<String> first(String input, Pattern pattern) {
        if (input == null) return Optional.empty();
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Optional.of(matcher.group().trim()) : Optional.empty();
    }

    public static String extractEmail(String input) { return first(input, RegexPatterns.EMAIL).orElse(""); }
    public static String extractPhone(String input) { return first(input, RegexPatterns.PHONE).orElse(""); }
    public static String extractPrice(String input) { return first(input, RegexPatterns.PRICE).orElse(""); }
    public static String extractSku(String input) { return first(input, RegexPatterns.SKU).orElse(""); }

    public static boolean isEmail(String input) {
        return input != null && RegexPatterns.EMAIL.matcher(input).matches();
    }

    /** Digits-only normalisation of a Bangladeshi phone number to 01XXXXXXXXX. */
    public static String normalizePhone(String input) {
        String phone = extractPhone(input).replaceAll("[^0-9]", "");
        if (phone.startsWith("880")) phone = "0" + phone.substring(3);
        return phone;
    }

    /** Luhn checksum, used by the payment-domain parser to reject invalid card numbers. */
    public static boolean passesLuhn(String number) {
        if (number == null) return false;
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.length() < 13 || digits.length() > 19) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
