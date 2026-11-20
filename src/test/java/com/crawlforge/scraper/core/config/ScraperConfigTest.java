package com.crawlforge.scraper.core.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.exception.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScraperConfigTest {

    @Test
    void malformedNumbersAreWrappedAsConfigurationErrors() throws IOException {
        Path file = Files.createTempFile("bad-config", ".properties");
        Files.writeString(file, "scraper.maxThreads=not-a-number\n");
        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> ScraperConfig.load(file));
        assertTrue(error.errorCode().equals("CONFIG_ERROR"));
    }

    @Test
    void nonFiniteRateIsRejected() throws IOException {
        Path file = Files.createTempFile("bad-rate", ".properties");
        Files.writeString(file, "scraper.rate.requestsPerSecond=NaN\n");
        assertThrows(ConfigurationException.class, () -> ScraperConfig.load(file));
    }
}
