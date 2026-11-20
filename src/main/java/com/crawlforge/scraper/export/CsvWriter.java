package com.crawlforge.scraper.export;

import com.crawlforge.scraper.core.exception.StorageException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/** CSV export using Apache Commons CSV. */
public final class CsvWriter implements DataWriter {

    @Override public String extension() { return "csv"; }

    private static void writeEmpty(String target) {
        try {
            Path path = Path.of(target);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, "");
        } catch (IOException e) {
            throw new StorageException("Could not write empty CSV to " + target, e);
        }
    }

    /** Prevent spreadsheet formula injection from scraped values. */
    private static String safe(String value) {
        return value.matches("^[=+\\-@].*") ? "'" + value : value;
    }

    @Override
    public void write(List<ScrapedRecord> records, String target) {
        if (records.isEmpty()) {
            writeEmpty(target);
            return;
        }
        Set<String> headers = new LinkedHashSet<>();
        records.forEach(record -> headers.addAll(record.toMap().keySet()));
        String[] headerArray = headers.toArray(String[]::new);
        try (StringWriter buffer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(buffer, CSVFormat.DEFAULT.builder()
                     .setHeader(headerArray).build())) {
            for (ScrapedRecord record : records) {
                var map = record.toMap();
                printer.printRecord(java.util.Arrays.stream(headerArray)
                        .map(h -> safe(map.getOrDefault(h, ""))).toList());
            }
            printer.flush();
            Path path = Path.of(target);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, buffer.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not write CSV to " + target, e);
        }
    }
}
