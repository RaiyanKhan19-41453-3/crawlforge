package com.crawlforge.scraper.http;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Thread-safe token bucket used to cap requests per second across all crawler threads. */
public final class RateLimiter {

    private final double permitsPerSecond;
    private final double capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();
    private double tokens;
    private long lastRefillNanos;

    public RateLimiter(double permitsPerSecond) {
        if (permitsPerSecond <= 0) throw new IllegalArgumentException("rate must be positive");
        this.permitsPerSecond = permitsPerSecond;
        this.capacity = Math.max(1.0, permitsPerSecond);
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    /** Blocks until one permit is available. Interruptions are propagated to the caller. */
    public void acquire() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            for (;;) {
                refill();
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return;
                }
                long waitNanos = (long) ((1.0 - tokens) / permitsPerSecond * 1_000_000_000L);
                available.awaitNanos(Math.max(1L, waitNanos));
            }
        } finally {
            lock.unlock();
        }
    }

    /** Non-blocking variant used by tests and schedulers. */
    public boolean tryAcquire() {
        if (!lock.tryLock()) return false;
        try {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * permitsPerSecond);
        lastRefillNanos = now;
    }
}
