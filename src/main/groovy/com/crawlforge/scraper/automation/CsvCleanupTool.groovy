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
                // Use positional access so duplicate header names never lose a column.
                printer.printRecord((0..<headers.size()).collect { i ->
                    row.size() > i ? row.get(i).trim().replaceAll(/\s+/, ' ') : ''
                })
            }
            printer.flush()
            printer.close()
        }
        writer.toString()
    }
}
