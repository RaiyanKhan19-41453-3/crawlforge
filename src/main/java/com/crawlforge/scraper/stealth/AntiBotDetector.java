package com.crawlforge.scraper.stealth;

import java.util.regex.Pattern;

/** Classifies responses as clean, suspicious, a CAPTCHA challenge or a hard block. */
public final class AntiBotDetector {

    public enum ThreatLevel { CLEAN, SUSPICIOUS, CAPTCHA, BLOCKED }

    private static final Pattern CAPTCHA = Pattern.compile(
            "(?i)(captcha|recaptcha|hcaptcha|cf-challenge|verify you are human|are you a robot|"
                    + "g-recaptcha|turnstile|prove you are human)");
    private static final Pattern BLOCKED = Pattern.compile(
            "(?i)(access denied|forbidden|unusual traffic|request blocked|bot detected|"
                    + "automated requests|rate limit exceeded|cloudflare ray id)");
    private static final Pattern SUSPICIOUS = Pattern.compile(
            "(?i)(please wait|checking your browser|temporarily unavailable|enable javascript)");

    public ThreatLevel detect(String body, int statusCode) {
        if (statusCode == 401 || statusCode == 403 || statusCode == 429) return ThreatLevel.BLOCKED;
        if (body == null || body.isBlank()) return ThreatLevel.CLEAN;
        String head = body.length() > 20_000 ? body.substring(0, 20_000) : body;
        if (CAPTCHA.matcher(head).find()) return ThreatLevel.CAPTCHA;
        if (BLOCKED.matcher(head).find()) return ThreatLevel.BLOCKED;
        if (SUSPICIOUS.matcher(head).find()) return ThreatLevel.SUSPICIOUS;
        return ThreatLevel.CLEAN;
    }

    public ThreatLevel detect(String body) { return detect(body, 200); }
}
