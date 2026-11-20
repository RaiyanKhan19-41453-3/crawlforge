package com.crawlforge.scraper.core.exception;

/** A permanent HTTP failure (401/403/404) that retrying cannot fix. */
public class NonRetryableFetchException extends FetchException {
    public NonRetryableFetchException(String message) { super(message); }
}
