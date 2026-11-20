package com.crawlforge.scraper.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void tokenBucketRefillsOverTime() {
        RateLimiter limiter = new RateLimiter(0.5); // one token every two seconds
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    void highRateAllowsBurstWithinCapacity() {
        RateLimiter limiter = new RateLimiter(1000);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
    }
}
