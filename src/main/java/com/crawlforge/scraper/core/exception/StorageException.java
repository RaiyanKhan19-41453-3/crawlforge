package com.crawlforge.scraper.core.exception;

public class StorageException extends ScraperException {
    public StorageException(String message) { super("STORAGE_ERROR", message); }
    public StorageException(String message, Throwable cause) { super("STORAGE_ERROR", message, cause); }
}
