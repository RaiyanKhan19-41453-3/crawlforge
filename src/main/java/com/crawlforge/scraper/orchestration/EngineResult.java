package com.crawlforge.scraper.orchestration;

import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.util.List;
import java.util.Map;

/** Outcome of a full run: records plus the metrics needed for a client report. */
public record EngineResult(List<ScrapedRecord> records, Map<String, Long> metrics, long elapsedMs) {

    public EngineResult {
        records = List.copyOf(records);
        metrics = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(metrics));
    }

    @Override
    public String toString() {
        return "EngineResult{records=" + records.size() + ", elapsedMs=" + elapsedMs + ", metrics=" + metrics + '}';
    }
}
