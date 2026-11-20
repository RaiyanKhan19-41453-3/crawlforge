package com.crawlforge.scraper.automation

import com.crawlforge.scraper.core.model.ScrapedRecord

/** Runnable Groovy tool: `java -cp crawlforge.jar com.crawlforge.scraper.automation.AutomationRunner`. */
class AutomationRunner {

    static void main(String[] args) {
        def sample = [
                ScrapedRecord.builder().source('demo').url('http://example.com/1')
                        .title('Widget A').content('A useful widget').qualityScore(1d).build(),
                ScrapedRecord.builder().source('demo').url('http://example.com/2')
                        .title('Widget B').content('Another widget').qualityScore(0.8d).build()
        ] as List<ScrapedRecord>

        println 'Data quality: ' + DataQualityReport.summarize(sample)
        println 'Cleaned CSV:\n' + CsvCleanupTool.clean('id,name\n1,  Widget   A  \n2,Widget B\n')
        println 'Integration payload:\n' + IntegrationPayloadBuilder.build(sample)
    }
}
