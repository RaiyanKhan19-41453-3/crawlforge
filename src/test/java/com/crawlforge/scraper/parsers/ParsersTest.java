package com.crawlforge.scraper.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crawlforge.scraper.core.config.SiteConfig;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import com.crawlforge.scraper.core.model.SourceType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class ParsersTest {

    @Test
    void parsesJsonWithJsonPointer() {
        SiteConfig site = SiteConfig.builder().name("api").sourceType(SourceType.JSON)
                .baseUrl("http://api.test").jsonResultPath("$.products")
                .field("sku", "id").field("title", "title").build();
        String json = "{\"products\":[{\"id\":\"SKU-1\",\"title\":\"Widget A\"},"
                + "{\"id\":\"SKU-2\",\"title\":\"Widget B\"}]}";
        List<ScrapedRecord> records = new JsonParser().parse(json, site);
        assertEquals(2, records.size());
        assertEquals("SKU-1", records.get(0).attributes().get("sku"));
    }

    @Test
    void parsesXmlWithXPath() {
        SiteConfig site = SiteConfig.builder().name("feed").sourceType(SourceType.XML)
                .baseUrl("http://feed.test").itemSelector("//item")
                .field("title", "title/text()").field("price", "price/text()").build();
        String xml = "<catalog><item><title>Widget A</title><price>1250</price></item>"
                + "<item><title>Widget B</title><price>899</price></item></catalog>";
        List<ScrapedRecord> records = new XmlParser().parse(xml, site);
        assertEquals(2, records.size());
        assertEquals("Widget A", records.get(0).attributes().get("title"));
    }

    @Test
    void parsesCsvWithHeaders() {
        SiteConfig site = SiteConfig.builder().name("csv").sourceType(SourceType.CSV)
                .baseUrl("http://csv.test").build();
        List<ScrapedRecord> records = new CsvParser()
                .parse("id,title,price\nSKU-1,Widget A,1250\nSKU-2,Widget B,899\n", site);
        assertEquals(2, records.size());
        assertEquals("Widget B", records.get(1).attributes().get("title"));
    }

    @Test
    void parsesGeneratedPdf() throws Exception {
        PDDocument pdf = new PDDocument();
        PDPage page = new PDPage();
        pdf.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(50, 700);
            stream.showText("Hello PDF Widget");
            stream.endText();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdf.save(out);
        pdf.close();

        SiteConfig site = SiteConfig.builder().name("pdf").sourceType(SourceType.PDF)
                .baseUrl("http://pdf.test/doc.pdf").build();
        List<ScrapedRecord> records = new PdfParser()
                .parse(new String(out.toByteArray(), StandardCharsets.ISO_8859_1), site);
        assertEquals(1, records.size());
        assertTrue(records.get(0).content().contains("Hello PDF Widget"));
    }
}
