package com.crawlforge.scraper.export;

import com.crawlforge.scraper.core.exception.StorageException;

/** Selects an output writer from a file extension. */
public final class WriterFactory {

    private WriterFactory() {}

    public static DataWriter forTarget(String target) {
        String lower = target.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".csv")) return new CsvWriter();
        if (lower.endsWith(".json")) return new JsonWriter();
        if (lower.endsWith(".xlsx")) return new ExcelWriter();
        throw new StorageException("Unsupported output format: " + target);
    }
}
