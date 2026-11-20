package com.crawlforge.scraper.automation

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter

/** Groovy automation script: normalises whitespace in a client-supplied CSV feed. */
class CsvCleanupTool {

    static String clean(String input) {
        def readerFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build()
        def writer = new StringWriter()
        CSVParser.parse(input, readerFormat).withCloseable { parser ->
            List<String> headers = parser.headerNames
            def printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers as String[]).build())
            parser.each { row ->
                printer.printRecord(headers.collect { h ->
                    row.isMapped(h) ? row.get(h).trim().replaceAll(/\s+/, ' ') : ''
                })
            }
            printer.flush()
            printer.close()
        }
        writer.toString()
    }
}
