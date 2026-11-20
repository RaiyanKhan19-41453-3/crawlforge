package com.crawlforge.scraper.core.exception;

public class ParseException extends ScraperException {
    public ParseException(String message) { super("PARSE_ERROR", message); }
    public ParseException(String message, Throwable cause) { super("PARSE_ERROR", message, cause); }
}
