package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/** CSV extraction with Apache Commons CSV and header-based field mapping. */
public final class CsvParser implements Parser {

    @Override
    public boolean supports(SourceType type) { return type == SourceType.CSV; }

    @Override
    public List<ScrapedRecord> parse(String raw, SiteConfig site) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build();
        try (CSVParser csv = CSVParser.parse(raw, format)) {
            List<ScrapedRecord> results = new ArrayList<>();
            int rowNumber = 0;
            for (CSVRecord row : csv) {
                rowNumber++;
                Map<String, String> attributes = new LinkedHashMap<>();
                for (String header : csv.getHeaderNames()) {
                    attributes.put(header, DataNormalizer.normalize(row.isMapped(header) ? row.get(header) : ""));
                }
                site.fields().forEach((name, column) -> {
                    if (attributes.containsKey(column)) attributes.putIfAbsent(name, attributes.get(column));
                });
                String url = site.baseUrl() == null ? "csv://row/" + rowNumber
                        : site.baseUrl() + "#row-" + rowNumber;
                String title = attributes.getOrDefault("title", "");
                String content = String.join(" ", attributes.values());
                results.add(ScrapedRecord.builder()
                        .source(site.name())
                        .url(url)
                        .title(title)
                        .content(DataNormalizer.truncate(content, 10_000))
                        .attributes(attributes)
                        .qualityScore(DataValidator.score(url, content, attributes))
                        .build());
            }
            return List.copyOf(results);
        } catch (IOException | IllegalArgumentException e) {
            throw new ParseException("Could not parse CSV for site " + site.name(), e);
        }
    }
}
