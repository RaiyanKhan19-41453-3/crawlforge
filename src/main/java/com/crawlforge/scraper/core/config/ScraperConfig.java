package com.crawlforge.scraper.core.config;

import com.crawlforge.scraper.core.exception.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Runtime configuration loaded from application.properties with safe defaults. */
public final class ScraperConfig {

    private final int maxThreads;
    private final int timeoutMs;
    private final RetryPolicy retryPolicy;
    private final double requestsPerSecond;
    private final boolean headless;
    private final boolean javascriptEnabled;
    private final String outputDir;
    private final List<String> userAgents;
    private final List<String> proxies;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private ScraperConfig(Properties p) {
        this.maxThreads = integer(p, "scraper.maxThreads", 4);
        this.timeoutMs = integer(p, "scraper.timeoutMs", 15_000);
        int attempts = integer(p, "scraper.retry.attempts", 3);
        long base = longValue(p, "scraper.retry.baseDelayMs", 500);
        long max = longValue(p, "scraper.retry.maxDelayMs", 8_000);
        boolean exp = bool(p, "scraper.retry.exponential", true);
        this.retryPolicy = exp ? RetryPolicy.exponential(attempts, base, max) : RetryPolicy.fixed(attempts, base);
        this.requestsPerSecond = doubleValue(p, "scraper.rate.requestsPerSecond", 2.0);
        this.headless = bool(p, "scraper.browser.headless", true);
        this.javascriptEnabled = bool(p, "scraper.browser.javascriptEnabled", false);
        this.outputDir = p.getProperty("scraper.outputDir", "output");
        this.userAgents = list(p, "scraper.userAgents");
        this.proxies = list(p, "scraper.proxies");
        this.dbUrl = p.getProperty("scraper.db.url", "");
        this.dbUser = p.getProperty("scraper.db.user", "");
        this.dbPassword = p.getProperty("scraper.db.password", "");

        if (maxThreads < 1) throw new ConfigurationException("scraper.maxThreads must be >= 1");
        if (timeoutMs < 1) throw new ConfigurationException("scraper.timeoutMs must be >= 1");
        if (requestsPerSecond <= 0) throw new ConfigurationException("rate must be positive");
    }

    public static ScraperConfig defaults() { return new ScraperConfig(new Properties()); }

    public static ScraperConfig load(Path path) {
        Properties props = new Properties();
        try {
            if (path != null && Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) { props.load(in); }
            } else {
                try (InputStream in = ScraperConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
                    if (in != null) props.load(in);
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException("Could not load configuration from " + path, e);
        }
        // Environment overrides make the same jar deployable on any VDI / client platform.
        override(props, "SCRAPER_MAX_THREADS", "scraper.maxThreads");
        override(props, "SCRAPER_DB_URL", "scraper.db.url");
        override(props, "SCRAPER_DB_USER", "scraper.db.user");
        override(props, "SCRAPER_DB_PASSWORD", "scraper.db.password");
        return new ScraperConfig(props);
    }

    private static void override(Properties props, String env, String key) {
        String value = System.getenv(env);
        if (value != null && !value.isBlank()) props.setProperty(key, value);
    }

    private static int integer(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid integer for " + key + ": " + p.getProperty(key), e);
        }
    }
    private static long longValue(Properties p, String key, long def) {
        try {
            return Long.parseLong(p.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid long for " + key + ": " + p.getProperty(key), e);
        }
    }
    private static double doubleValue(Properties p, String key, double def) {
        try {
            double value = Double.parseDouble(p.getProperty(key, String.valueOf(def)));
            if (!Double.isFinite(value)) throw new NumberFormatException("not finite");
            return value;
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid number for " + key + ": " + p.getProperty(key), e);
        }
    }
    private static boolean bool(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)));
    }
    private static List<String> list(Properties p, String key) {
        String raw = p.getProperty(key, "");
        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (!part.isBlank()) values.add(part.trim());
        }
        return List.copyOf(values);
    }

    public int maxThreads() { return maxThreads; }
    public int timeoutMs() { return timeoutMs; }
    public RetryPolicy retryPolicy() { return retryPolicy; }
    public double requestsPerSecond() { return requestsPerSecond; }
    public boolean headless() { return headless; }
    public boolean javascriptEnabled() { return javascriptEnabled; }
    public String outputDir() { return outputDir; }
    public List<String> userAgents() { return userAgents; }
    public List<String> proxies() { return proxies; }
    public String dbUrl() { return dbUrl; }
    public String dbUser() { return dbUser; }
    public String dbPassword() { return dbPassword; }
    public boolean databaseEnabled() { return !dbUrl.isBlank(); }

    @Override
    public String toString() {
        return "ScraperConfig{threads=" + maxThreads + ", timeout=" + timeoutMs + "ms, "
                + retryPolicy + ", rps=" + requestsPerSecond + ", db=" + databaseEnabled() + '}';
    }
}
