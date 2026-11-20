package com.crawlforge.scraper.persistence;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.exception.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDBC connection factory supporting PostgreSQL, MySQL and H2 (tests). */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private final ScraperConfig config;

    public DatabaseManager(ScraperConfig config) { this.config = config; }

    public Connection connection() {
        if (!config.databaseEnabled()) {
            throw new StorageException("No database URL configured (scraper.db.url)");
        }
        try {
            return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
        } catch (SQLException e) {
            throw new StorageException("Could not connect to " + config.dbUrl(), e);
        }
    }

    /** Applies an idempotent schema script. Used on VDI and client platforms before a run. */
    public void initializeSchema(Path sqlFile) {
        try {
            String script = Files.readString(sqlFile);
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                for (String sql : script.split(";")) {
                    if (!sql.isBlank()) statement.execute(sql);
                }
            }
            log.info("Database schema initialised from {}", sqlFile);
        } catch (IOException | SQLException e) {
            throw new StorageException("Could not initialise schema from " + sqlFile, e);
        }
    }
}
