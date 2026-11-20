package com.crawlforge.scraper.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void tokenBucketRefillsOverTime() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1.0); // capacity one token
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        Thread.sleep(1_100);
        assertTrue(limiter.tryAcquire(), "the bucket should have refilled after one second");
    }

    @Test
    void highRateAllowsABurstWithinCapacity() {
        RateLimiter limiter = new RateLimiter(10);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire());
        }
    }
}
