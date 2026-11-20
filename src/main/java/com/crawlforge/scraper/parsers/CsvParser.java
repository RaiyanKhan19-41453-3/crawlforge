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
        raw = DataNormalizer.stripBom(raw);
        CSVFormat.Builder formatBuilder = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true);
        if (site.csvHasHeader()) {
            formatBuilder.setHeader().setSkipHeaderRecord(true);
        }
        CSVFormat format = formatBuilder.build();
        try (CSVParser csv = CSVParser.parse(raw, format)) {
            List<ScrapedRecord> results = new ArrayList<>();
            int rowNumber = 0;
            for (CSVRecord row : csv) {
                rowNumber++;
                Map<String, String> attributes = new LinkedHashMap<>();
                if (site.csvHasHeader()) {
                    for (String header : csv.getHeaderNames()) {
                        attributes.put(header, DataNormalizer.normalize(row.isMapped(header) ? row.get(header) : ""));
                    }
                    site.fields().forEach((name, column) -> {
                        if (attributes.containsKey(column)) attributes.putIfAbsent(name, attributes.get(column));
                    });
                } else {
                    // Headerless feeds: columns are addressed by zero-based index in the recipe.
                    for (int i = 0; i < row.size(); i++) {
                        attributes.put("col_" + (i + 1), DataNormalizer.normalize(row.get(i)));
                    }
                    site.fields().forEach((name, selector) -> {
                        try {
                            int index = Integer.parseInt(selector);
                            if (index >= 0 && index < row.size()) {
                                attributes.put(name, DataNormalizer.normalize(row.get(index)));
                            }
                        } catch (NumberFormatException ignored) {
                            // Non-numeric selectors are only valid for headed CSV files.
                        }
                    });
                }
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
