package com.crawlforge.scraper.automation

import com.crawlforge.scraper.core.model.ScrapedRecord

/** Groovy reporting tool used by the data-entry QA team. */
class DataQualityReport {

    static Map<String, Object> summarize(List<ScrapedRecord> records) {
        if (!records) return [total: 0, valid: 0, averageQuality: 0d, lowQuality: 0]

        double total = records.collect { it.qualityScore() }.sum() as double
        return [
                total         : records.size(),
                valid         : records.count { it.isValid() },
                averageQuality: (total / records.size()).round(3),
                lowQuality    : records.count { it.qualityScore() < 0.7d }
        ]
    }
}
