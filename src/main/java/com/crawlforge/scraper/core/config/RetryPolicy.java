package com.crawlforge.scraper.core.config;

import java.time.Duration;

/** Backoff strategy for transient fetch failures. Immutable and thread-safe. */
public final class RetryPolicy {

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final boolean exponential;

    public RetryPolicy(int maxAttempts, long baseDelayMs, long maxDelayMs, boolean exponential) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (baseDelayMs < 0) throw new IllegalArgumentException("baseDelayMs must be >= 0");
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = Math.max(baseDelayMs, maxDelayMs);
        this.exponential = exponential;
    }

    public static RetryPolicy fixed(int attempts, long delayMs) {
        return new RetryPolicy(attempts, delayMs, delayMs, false);
    }

    public static RetryPolicy exponential(int attempts, long baseDelayMs, long maxDelayMs) {
        return new RetryPolicy(attempts, baseDelayMs, maxDelayMs, true);
    }

    public int maxAttempts() { return maxAttempts; }
    public boolean shouldRetry(int attemptNumber) { return attemptNumber < maxAttempts; }

    /** Delay before the next attempt. attemptNumber is 1-based. */
    public Duration delayFor(int attemptNumber) {
        long multiplier = exponential ? (1L << Math.min(Math.max(attemptNumber - 1, 0), 16)) : 1L;
        long delay = baseDelayMs > Long.MAX_VALUE / multiplier ? maxDelayMs : baseDelayMs * multiplier;
        return Duration.ofMillis(Math.min(delay, maxDelayMs));
    }

    @Override
    public String toString() {
        return "RetryPolicy{attempts=" + maxAttempts + ", base=" + baseDelayMs + "ms, "
                + (exponential ? "exponential" : "fixed") + ", max=" + maxDelayMs + "ms}";
    }
}
