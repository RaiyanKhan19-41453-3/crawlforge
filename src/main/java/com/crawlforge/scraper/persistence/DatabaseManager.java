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
                for (String sql : splitStatements(script)) {
                    statement.execute(sql);
                }
            }
            log.info("Database schema initialized from {}", sqlFile);
        } catch (IOException | SQLException e) {
            throw new StorageException("Could not initialise schema from " + sqlFile, e);
        }
    }

    /** Splits on semicolons that are actual statement separators, not inside quotes/comments. */
    static java.util.List<String> splitStatements(String script) {
        java.util.List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false, inDouble = false, inLineComment = false, inBlockComment = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';
            if (inLineComment) {
                current.append(c);
                if (c == '\n' || c == '\r') inLineComment = false;
            } else if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
            } else if (inSingle) {
                current.append(c);
                if (c == '\'') inSingle = false;
            } else if (inDouble) {
                current.append(c);
                if (c == '"') inDouble = false;
            } else if (c == '-' && next == '-') {
                inLineComment = true;
                current.append(c);
            } else if (c == '/' && next == '*') {
                inBlockComment = true;
                current.append(c);
            } else if (c == '\'') {
                inSingle = true;
                current.append(c);
            } else if (c == '"') {
                inDouble = true;
                current.append(c);
            } else if (c == ';') {
                addIfSql(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfSql(statements, current);
        return statements;
    }

    private static void addIfSql(java.util.List<String> statements, StringBuilder fragment) {
        String withoutComments = fragment.toString()
                .replaceAll("(?s)--[^\\n]*", "")
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .trim();
        if (!withoutComments.isEmpty()) statements.add(fragment.toString().trim());
    }
}
