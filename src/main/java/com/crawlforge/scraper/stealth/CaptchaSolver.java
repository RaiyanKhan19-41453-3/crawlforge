package com.crawlforge.scraper.stealth;

/** Pluggable strategy for CAPTCHA challenges. */
public interface CaptchaSolver {

    /**
     * Attempt to resolve a challenge.
     *
     * @return true when the challenge was handled and the request may be retried
     */
    boolean solve(String url, String challengeHtml);

    /** Default implementation: records the challenge for a human/outsourced review queue. */
    CaptchaSolver QUEUE = (url, html) -> {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CaptchaSolver.class);
        log.warn("CAPTCHA challenge queued for review: {} (html bytes={})", url,
                html == null ? 0 : html.length());
        return false;
    };
}
