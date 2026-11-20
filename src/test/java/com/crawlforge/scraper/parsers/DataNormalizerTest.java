package com.crawlforge.scraper.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DataNormalizerTest {

    @Test
    void collapsesWhitespaceAndNonBreakingSpaces() {
        assertEquals("Widget A price", DataNormalizer.normalize("  Widget\u00a0A   price \n"));
    }

    @Test
    void stripsHtmlTags() {
        assertEquals("Widget A", DataNormalizer.stripHtml("<div><b>Widget</b> A</div>"));
    }

    @Test
    void normalizesThousandSeparatedNumbers() {
        assertEquals("1250.00", DataNormalizer.numeric("1,250.00"));
    }
}
