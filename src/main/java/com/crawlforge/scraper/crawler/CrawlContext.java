package com.crawlforge.scraper.crawler;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** Mutable per-job traversal state. Thread-safe so parallel workers can share the frontier. */
public final class CrawlContext {

    private final Queue<String> frontier = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> depths = new ConcurrentHashMap<>();
    private final AtomicInteger pages = new AtomicInteger();
    private final AtomicInteger records = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    public boolean enqueue(String url, int depth) {
        if (url == null || url.isBlank() || visited.contains(url)) return false;
        if (visited.add(url)) {
            frontier.add(url);
            depths.put(url, depth);
            return true;
        }
        return false;
    }

    public String poll() { return frontier.poll(); }
    public int depthOf(String url) { return depths.getOrDefault(url, 0); }
    public boolean isEmpty() { return frontier.isEmpty(); }
    public int frontierSize() { return frontier.size(); }
    public int visitedCount() { return visited.size(); }

    public void recordPage(int extracted) {
        pages.incrementAndGet();
        records.addAndGet(extracted);
    }
    public void recordFailure() { failures.incrementAndGet(); }
    public int pages() { return pages.get(); }
    public int records() { return records.get(); }
    public int failures() { return failures.get(); }
}
