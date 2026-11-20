package com.crawlforge.scraper.http;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe round-robin User-Agent rotation; part of the anti-bot toolkit. */
public final class UserAgentRotator {

    public static final List<String> DEFAULTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    );

    private final List<String> agents;
    private final AtomicInteger cursor = new AtomicInteger();

    public UserAgentRotator(List<String> agents) {
        this.agents = agents == null || agents.isEmpty() ? DEFAULTS : List.copyOf(agents);
    }

    public String next() {
        int index = Math.floorMod(cursor.getAndIncrement(), agents.size());
        return agents.get(index);
    }

    public int size() { return agents.size(); }
}
