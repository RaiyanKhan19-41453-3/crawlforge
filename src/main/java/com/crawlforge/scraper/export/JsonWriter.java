package com.crawlforge.scraper.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.crawlforge.scraper.core.exception.StorageException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** JSON export with a stable, client-friendly envelope. */
public final class JsonWriter implements DataWriter {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override public String extension() { return "json"; }

    @Override
    public void write(List<ScrapedRecord> records, String target) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("generatedBy", "CrawlForge");
            root.put("count", records.size());
            ArrayNode items = root.putArray("records");
            for (ScrapedRecord record : records) {
                ObjectNode node = items.addObject();
                node.put("source", record.source());
                node.put("url", record.url());
                node.put("title", record.title());
                node.put("content", record.content());
                node.put("qualityScore", record.qualityScore());
                node.put("scrapedAt", record.scrapedAt().toString());
                ObjectNode fields = node.putObject("attributes");
                record.attributes().forEach(fields::put);
            }
            Path path = Path.of(target);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
        } catch (IOException e) {
            throw new StorageException("Could not write JSON to " + target, e);
        }
    }
}
