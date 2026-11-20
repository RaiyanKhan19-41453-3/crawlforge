package com.crawlforge.scraper.core.exception;

public class FetchException extends ScraperException {
    public FetchException(String message) { super("FETCH_ERROR", message); }
    public FetchException(String message, Throwable cause) { super("FETCH_ERROR", message, cause); }
}
