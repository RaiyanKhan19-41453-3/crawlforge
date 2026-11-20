package com.crawlforge.scraper.orchestration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Re-runs a crawl on a fixed cadence, as a client would request for frequently changing sites. */
public final class JobScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void schedule(Runnable job, long initialDelay, long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                job.run();
            } catch (RuntimeException e) {
                log.error("Scheduled crawl failed: {}", e.getMessage());
            }
        }, initialDelay, period, unit);
    }

    @Override
    public void close() { scheduler.shutdownNow(); }
}
