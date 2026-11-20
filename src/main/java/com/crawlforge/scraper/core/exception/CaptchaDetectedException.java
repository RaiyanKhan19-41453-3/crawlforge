package com.crawlforge.scraper.core.exception;

public class CaptchaDetectedException extends AntiBotException {
    public CaptchaDetectedException(String message) { super(message); }
    public CaptchaDetectedException(String message, Throwable cause) { super(message, cause); }
}
