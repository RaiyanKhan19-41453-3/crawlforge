package com.crawlforge.scraper.core.exception;

/** Base type for every expected failure, carrying a stable error code for support runbooks. */
public class ScraperException extends RuntimeException {

    private final String errorCode;

    public ScraperException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ScraperException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() { return errorCode; }
}
