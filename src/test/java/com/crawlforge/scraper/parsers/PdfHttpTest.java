package com.crawlforge.scraper.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.ScraperConfig;
import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapeJob;
import com.crawlforge.scraper.core.model.SourceType;
import com.crawlforge.scraper.crawler.CrawlerFactory;
import com.crawlforge.scraper.http.ResilientHttpClient;
import com.crawlforge.scraper.orchestration.EngineResult;
import com.crawlforge.scraper.orchestration.ScrapeEngine;
import com.crawlforge.scraper.testsupport.LocalServer;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

/** Proves binary PDF bytes survive the HTTP transport, not just direct parser input. */
class PdfHttpTest {

    @Test
    void crawlsPdfFetchedOverHttp() throws Exception {
        PDDocument pdf = new PDDocument();
        PDPage page = new PDPage();
        pdf.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(50, 700);
            stream.showText("Binary PDF over HTTP");
            stream.endText();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdf.save(out);
        pdf.close();
        byte[] pdfBytes = out.toByteArray();

        try (LocalServer server = LocalServer.start()) {
            server.route("/doc.pdf", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.sendResponseHeaders(200, pdfBytes.length);
                exchange.getResponseBody().write(pdfBytes);
                exchange.close();
            });

            Path properties = Files.createTempFile("pdf-http", ".properties");
            Files.writeString(properties, "scraper.retry.attempts=1\nscraper.rate.requestsPerSecond=100\n");
            ScraperConfig config = ScraperConfig.load(properties);

            SiteConfig site = SiteConfig.builder()
                    .name("pdf-site")
                    .sourceType(SourceType.PDF)
                    .baseUrl(server.url(""))
                    .seedUrl(server.url("/doc.pdf"))
                    .followLinks(false)
                    .maxPages(1)
                    .build();

            try (ResilientHttpClient http = new ResilientHttpClient(config)) {
                ScrapeEngine engine = new ScrapeEngine(config,
                        new CrawlerFactory(config, http, new ParserRegistry()));
                EngineResult result = engine.execute(
                        ScrapeJob.builder().name("pdf-http-test").site(site).build());
                assertEquals(1, result.records().size());
                assertTrue(result.records().get(0).content().contains("Binary PDF over HTTP"));
            }
        }
    }
}
