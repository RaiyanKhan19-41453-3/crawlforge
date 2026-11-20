package com.crawlforge.scraper.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WriterTest {

    @TempDir Path tempDir;

    private List<ScrapedRecord> records() {
        return List.of(
                ScrapedRecord.builder().source("demo").url("http://demo.test/1").title("Widget A")
                        .content("First widget").attribute("price", "100").build(),
                ScrapedRecord.builder().source("demo").url("http://demo.test/2").title("Widget B")
                        .content("Second widget").attribute("price", "200").build());
    }

    @Test
    void writesCsv() throws Exception {
        Path target = tempDir.resolve("records.csv");
        new CsvWriter().write(records(), target.toString());
        String content = Files.readString(target);
        assertTrue(content.contains("Widget A"));
        assertTrue(content.contains("price"));
    }

    @Test
    void writesJson() throws Exception {
        Path target = tempDir.resolve("records.json");
        new JsonWriter().write(records(), target.toString());
        String content = Files.readString(target);
        assertTrue(content.contains("\"count\" : 2") || content.contains("\"count\":2"));
        assertTrue(content.contains("Widget B"));
    }

    @Test
    void writesExcel() throws Exception {
        Path target = tempDir.resolve("records.xlsx");
        new ExcelWriter().write(records(), target.toString());
        assertTrue(Files.size(target) > 0);
        assertEquals("xlsx", new ExcelWriter().extension());
    }

    @Test
    void writersHandleQuotesNewlinesAndEmoji() throws Exception {
        List<ScrapedRecord> special = List.of(
                ScrapedRecord.builder().source("demo").url("http://demo/1")
                        .title("A \"quoted\" title, with newline\nand 😀")
                        .content("line one\nline two").attribute("note", "comma, semicolon; tab\tend")
                        .build());
        for (String extension : List.of("csv", "json", "xlsx")) {
            Path target = tempDir.resolve("special." + extension);
            WriterFactory.forTarget(target.toString()).write(special, target.toString());
            assertTrue(Files.size(target) > 0, extension + " file should not be empty");
        }
        String json = Files.readString(tempDir.resolve("special.json"));
        assertTrue(json.contains("😀") || json.contains("\\uD83D\\uDE00"));
        assertTrue(json.contains("line one\\nline two"));
    }

    @Test
    void factorySelectsWriterByExtension() {
        assertTrue(WriterFactory.forTarget("a.csv") instanceof CsvWriter);
        assertTrue(WriterFactory.forTarget("a.json") instanceof JsonWriter);
        assertTrue(WriterFactory.forTarget("a.xlsx") instanceof ExcelWriter);
    }
}
