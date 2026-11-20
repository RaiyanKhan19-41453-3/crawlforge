package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.exception.ParseException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** PDF text extraction with Apache PDFBox; one record per page. */
public final class PdfParser implements Parser {

    @Override
    public boolean supports(SourceType type) { return type == SourceType.PDF; }

    @Override
    public List<ScrapedRecord> parse(String raw, SiteConfig site) {
        try (PDDocument document = Loader.loadPDF(raw.getBytes(StandardCharsets.ISO_8859_1))) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ScrapedRecord> results = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = DataNormalizer.normalize(stripper.getText(document));
                Map<String, String> attributes = new LinkedHashMap<>();
                attributes.put("page", String.valueOf(page));
                attributes.put("content", text);
                String url = site.baseUrl() == null ? "pdf://page/" + page : site.baseUrl() + "#page=" + page;
                results.add(ScrapedRecord.builder()
                        .source(site.name())
                        .url(url)
                        .title("Page " + page)
                        .content(text)
                        .attributes(attributes)
                        .qualityScore(DataValidator.score(url, text, attributes))
                        .build());
            }
            return List.copyOf(results);
        } catch (IOException e) {
            throw new ParseException("Could not parse PDF for site " + site.name(), e);
        }
    }
}
