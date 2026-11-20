package com.crawlforge.scraper.core.exception;

public class RateLimitException extends AntiBotException {
    public RateLimitException(String message) { super("RATE_LIMIT", message); }
}
