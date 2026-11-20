package com.crawlforge.scraper.http;

/** Raw HTTP payload for binary formats such as PDF. */
public record BinaryResponse(byte[] body) {
    public BinaryResponse {
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() { return body.clone(); }
}
