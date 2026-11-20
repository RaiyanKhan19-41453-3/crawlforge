package com.crawlforge.scraper.export;

import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.util.List;

/** Output strategy. Implementations write to CSV, JSON, Excel or SQL. */
public interface DataWriter {

    String extension();

    void write(List<ScrapedRecord> records, String target);
}
