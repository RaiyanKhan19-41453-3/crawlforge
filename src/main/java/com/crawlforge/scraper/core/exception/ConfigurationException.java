package com.crawlforge.scraper.core.exception;

public class ConfigurationException extends ScraperException {
    public ConfigurationException(String message) { super("CONFIG_ERROR", message); }
    public ConfigurationException(String message, Throwable cause) { super("CONFIG_ERROR", message, cause); }
}
