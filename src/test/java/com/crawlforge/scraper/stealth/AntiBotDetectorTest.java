package com.crawlforge.scraper.stealth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.crawlforge.scraper.stealth.AntiBotDetector.ThreatLevel;
import org.junit.jupiter.api.Test;

class AntiBotDetectorTest {

    private final AntiBotDetector detector = new AntiBotDetector();

    @Test
    void classifiesCleanAndChallengedPages() {
        assertEquals(ThreatLevel.CLEAN, detector.detect("<html><body>Product list</body></html>"));
        assertEquals(ThreatLevel.CAPTCHA, detector.detect("<div class='g-recaptcha'></div>"));
        assertEquals(ThreatLevel.BLOCKED, detector.detect("<h1>Access denied</h1>"));
        assertEquals(ThreatLevel.BLOCKED, detector.detect("anything", 429));
    }
}
