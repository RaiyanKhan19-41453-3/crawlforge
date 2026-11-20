package com.crawlforge.scraper.core.exception;

public class CaptchaDetectedException extends AntiBotException {
    public CaptchaDetectedException(String message) { super("CAPTCHA_ERROR", message); }
    public CaptchaDetectedException(String message, Throwable cause) { super("CAPTCHA_ERROR", message, cause); }
}
