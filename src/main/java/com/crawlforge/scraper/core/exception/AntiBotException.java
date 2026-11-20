package com.crawlforge.scraper.core.exception;

/** Raised when a target responds with access-denied / bot-challenge content. */
public class AntiBotException extends ScraperException {
    public AntiBotException(String message) { super("ANTI_BOT", message); }
    public AntiBotException(String message, Throwable cause) { super("ANTI_BOT", message, cause); }

    /** Allows subclasses to emit their own specific error code. */
    protected AntiBotException(String code, String message) { super(code, message); }
    protected AntiBotException(String code, String message, Throwable cause) { super(code, message, cause); }
}
