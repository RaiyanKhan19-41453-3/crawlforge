package com.crawlforge.scraper.core.exception;

public class RetryExhaustedException extends FetchException {
    public RetryExhaustedException(String message, Throwable cause) { super(message, cause); }
}
