package com.crawlforge.scraper.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crawlforge.scraper.core.exception.StorageException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** JDBC repository: batched inserts and data-quality reporting. */
public final class RecordRepository {

    private static final String INSERT =
            "INSERT INTO scraped_records(source, url, title, content, quality_score, attributes, scraped_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private final DatabaseManager database;
    private final ObjectMapper mapper = new ObjectMapper();

    public RecordRepository(DatabaseManager database) { this.database = database; }

    /** Batch insert with a single transaction. */
    public int saveAll(List<ScrapedRecord> records) {
        if (records.isEmpty()) return 0;
        Connection connection = null;
        try {
            connection = database.connection();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                for (ScrapedRecord record : records) {
                    statement.setString(1, record.source());
                    statement.setString(2, record.url());
                    statement.setString(3, record.title());
                    statement.setString(4, record.content());
                    statement.setDouble(5, record.qualityScore());
                    statement.setString(6, mapper.writeValueAsString(record.attributes()));
                    statement.setTimestamp(7, Timestamp.from(record.scrapedAt()));
                    statement.addBatch();
                }
                int[] counts = statement.executeBatch();
                connection.commit();
                return java.util.Arrays.stream(counts).sum();
            }
        } catch (SQLException | JsonProcessingException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // The original failure is the one worth reporting.
                }
            }
            throw new StorageException("Could not persist " + records.size() + " records", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // Nothing useful to do at this point.
                }
            }
        }
    }

    public long count() {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM scraped_records");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0;
        } catch (SQLException e) {
            throw new StorageException("Could not count records", e);
        }
    }

    /** Simple quality report used by the data-quality dashboard. */
    public String qualitySummary() {
        try (Connection connection = database.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*), AVG(quality_score), MIN(quality_score) FROM scraped_records");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) return "no data";
            return "rows=" + result.getLong(1) + ", avgQuality=" + result.getDouble(2)
                    + ", minQuality=" + result.getDouble(3);
        } catch (SQLException e) {
            throw new StorageException("Could not read quality summary", e);
        }
    }
}
