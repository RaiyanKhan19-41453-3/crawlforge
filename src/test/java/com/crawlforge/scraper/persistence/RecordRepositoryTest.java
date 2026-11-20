package com.crawlforge.scraper.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordRepositoryTest {

    @Test
    void persistsAndReportsQualityWithH2() throws IOException {
        Path properties = Files.createTempFile("crawlforge-db", ".properties");
        Files.writeString(properties, "scraper.db.url=jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1\n"
                + "scraper.rate.requestsPerSecond=100\n");
        ScraperConfig config = ScraperConfig.load(properties);

        DatabaseManager database = new DatabaseManager(config);
        database.initializeSchema(Path.of(System.getProperty("user.dir"), "sql", "schema.sql"));

        RecordRepository repository = new RecordRepository(database);
        List<ScrapedRecord> records = List.of(
                ScrapedRecord.builder().source("test").url("http://test/1").title("A")
                        .content("content one").qualityScore(1.0).build(),
                ScrapedRecord.builder().source("test").url("http://test/2").title("B")
                        .content("content two").qualityScore(0.8).build());
        assertEquals(2, repository.saveAll(records));
        assertEquals(2, repository.count());
        assertTrue(repository.qualitySummary().contains("rows=2"));
    }
}
