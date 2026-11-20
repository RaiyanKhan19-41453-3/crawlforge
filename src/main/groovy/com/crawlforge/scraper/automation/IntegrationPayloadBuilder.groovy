package com.crawlforge.scraper.automation

import com.crawlforge.scraper.core.model.ScrapedRecord
import groovy.json.JsonBuilder

/** Groovy integration helper: turns scraped records into a client-ready JSON payload. */
class IntegrationPayloadBuilder {

    static String build(List<ScrapedRecord> records, String clientName = 'Client') {
        def payload = [
                generatedBy: 'CrawlForge-Groovy',
                client     : clientName,
                count      : records.size(),
                items      : records.collect { record ->
                    [
                            source : record.source(),
                            url    : record.url(),
                            title  : record.title(),
                            fields : record.attributes()
                    ]
                }
        ]
        new JsonBuilder(payload).toPrettyString()
    }
}
