package com.crawlforge.scraper.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.crawler.CrawlContext;
import com.crawlforge.scraper.http.CookieJar;
import com.crawlforge.scraper.payment.CardDataMasker;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.junit.jupiter.api.Test;

class CoreUtilitiesTest {

    @Test
    void schedulerRunsTheTask() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        try (JobScheduler scheduler = new JobScheduler()) {
            scheduler.schedule(counter::incrementAndGet, 50, 50, TimeUnit.MILLISECONDS);
            Thread.sleep(300);
        }
        assertTrue(counter.get() >= 2);
    }

    @Test
    void workerPoolRunsTasksAndShutsDown() throws Exception {
        ExecutorService pool = WorkerPoolExecutor.newPool(2);
        Future<Integer> result = pool.submit(() -> 42);
        assertEquals(42, result.get());
        WorkerPoolExecutor.shutdown(pool, 5);
        assertTrue(pool.isShutdown());
    }

    @Test
    void sharedPageBudgetIsAtomicAcrossContexts() {
        AtomicInteger budget = new AtomicInteger(3);
        CrawlContext first = new CrawlContext(budget);
        CrawlContext second = new CrawlContext(budget);
        int granted = 0;
        for (int i = 0; i < 3; i++) {
            if (first.tryReservePage()) granted++;
            if (second.tryReservePage()) granted++;
        }
        assertEquals(3, granted);
        assertFalse(first.tryReservePage());
        assertFalse(second.tryReservePage());
    }

    @Test
    void cardMaskingKeepsOnlyLastFourDigits() {
        assertEquals("**** **** **** 1111", CardDataMasker.mask("4111 1111 1111 1111"));
        assertEquals("VISA", CardDataMasker.brand("4111111111111111"));
    }

    @Test
    void cookieJarSupportsBulkAddAndClear() {
        BasicClientCookie cookie = new BasicClientCookie("sid", "abc");
        cookie.setDomain("example.test");
        CookieJar jar = new CookieJar();
        jar.addAll(List.of(cookie));
        assertEquals(1, jar.size());
        jar.clear();
        assertEquals(0, jar.size());
    }
}
