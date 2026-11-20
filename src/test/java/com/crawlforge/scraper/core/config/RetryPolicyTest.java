package com.crawlforge.scraper.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void exponentialBackoffIsBounded() {
        RetryPolicy policy = RetryPolicy.exponential(4, 100, 1000);
        assertEquals(Duration.ofMillis(100), policy.delayFor(1));
        assertEquals(Duration.ofMillis(200), policy.delayFor(2));
        assertEquals(Duration.ofMillis(1000), policy.delayFor(10));
        assertTrue(policy.shouldRetry(3));
        assertFalse(policy.shouldRetry(4));
    }

    @Test
    void fixedBackoffIsConstant() {
        RetryPolicy policy = RetryPolicy.fixed(3, 250);
        assertEquals(policy.delayFor(1), policy.delayFor(2));
    }
}
