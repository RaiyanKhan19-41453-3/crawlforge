package com.crawlforge.scraper.core.exception;

/** Raised when a target responds with access-denied / bot-challenge content. */
public class AntiBotException extends ScraperException {
    public AntiBotException(String message) { super("ANTI_BOT", message); }
    public AntiBotException(String message, Throwable cause) { super("ANTI_BOT", message, cause); }
}
