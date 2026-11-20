package com.crawlforge.scraper.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Proves Java/Groovy interoperability and that the Groovy tools actually work. */
class GroovyAutomationTest {

    @Test
    void groovyQualityReportWorksFromJava() {
        List<ScrapedRecord> records = List.of(
                ScrapedRecord.builder().source("demo").url("http://demo/1").content("A")
                        .qualityScore(1.0).build(),
                ScrapedRecord.builder().source("demo").url("http://demo/2").content("B")
                        .qualityScore(0.5).build());
        Map<String, Object> summary = DataQualityReport.summarize(records);
        assertEquals(2, summary.get("total"));
        assertEquals(1L, ((Number) summary.get("lowQuality")).longValue());
    }

    @Test
    void groovyCsvCleanupCollapsesWhitespace() {
        String cleaned = CsvCleanupTool.clean("id,name\n1,  Widget   A  \n2,Widget B\n");
        assertTrue(cleaned.contains("Widget A"));
        assertTrue(!cleaned.contains("   "));
    }

    @Test
    void groovyIntegrationPayloadContainsClientName() {
        String payload = IntegrationPayloadBuilder.build(List.of(), "Test Client");
        assertTrue(payload.contains("Test Client"));
        assertTrue(payload.contains("\"count\""));
    }
}
