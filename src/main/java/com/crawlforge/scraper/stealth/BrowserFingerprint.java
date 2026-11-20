package com.crawlforge.scraper.stealth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A consistent, human-like browser profile. Keeping the fingerprint stable across requests
 * matters more than randomising it: contradictory headers are a classic bot signal.
 */
public final class BrowserFingerprint {

    private final String userAgent;
    private final String platform;
    private final String languages;
    private final String timezone;
    private final String screen;
    private final int hardwareConcurrency;
    private final String webglVendor;

    public BrowserFingerprint(String userAgent, String platform, String languages, String timezone,
                              String screen, int hardwareConcurrency, String webglVendor) {
        this.userAgent = userAgent;
        this.platform = platform;
        this.languages = languages;
        this.timezone = timezone;
        this.screen = screen;
        this.hardwareConcurrency = hardwareConcurrency;
        this.webglVendor = webglVendor;
    }

    /** Deterministic default profile used by all browser automation engines. */
    public static BrowserFingerprint consistent() {
        return new BrowserFingerprint(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Win32",
                "en-US,en;q=0.9",
                "Asia/Dhaka",
                "1920x1080",
                8,
                "Google Inc. (NVIDIA)");
    }

    public String userAgent() { return userAgent; }
    public String platform() { return platform; }
    public String languages() { return languages; }
    public String timezone() { return timezone; }
    public String screen() { return screen; }
    public int hardwareConcurrency() { return hardwareConcurrency; }
    public String webglVendor() { return webglVendor; }

    /** Extra headers that make an HttpClient request look like a real browser. */
    public Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept-Language", languages);
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("DNT", "1");
        return headers;
    }

    /** Chrome/Selenium flags that suppress the most obvious automation markers. */
    public List<String> chromeArguments() {
        return List.of(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-blink-features=AutomationControlled",
                "--lang=en-US",
                "--window-size=" + screen.replace('x', ',')
        );
    }
}
