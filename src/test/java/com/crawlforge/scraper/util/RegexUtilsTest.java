package com.crawlforge.scraper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegexUtilsTest {

    @Test
    void extractsEmailAndPhone() {
        assertEquals("razvan@example.com", RegexUtils.extractEmail("Mail razvan@example.com now"));
        assertEquals("01712345678", RegexUtils.normalizePhone("Call +8801712345678 today"));
        assertTrue(RegexUtils.isEmail("test.user+tag@example.co.uk"));
        assertFalse(RegexUtils.isEmail("not-an-email"));
    }

    @Test
    void extractsSkuAndPrice() {
        assertEquals("SKU-1001", RegexUtils.extractSku("Product SKU-1001 in stock"));
        assertEquals("1,250.00", RegexUtils.extractPrice("BDT 1,250.00"));
    }

    @Test
    void luhnCheckAcceptsValidCardAndRejectsInvalid() {
        assertTrue(RegexUtils.passesLuhn("4111 1111 1111 1111"));
        assertFalse(RegexUtils.passesLuhn("4111 1111 1111 1112"));
    }
}
