package com.crawlforge.scraper.automation

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter

/** Groovy automation script: normalises whitespace in a client-supplied CSV feed. */
class CsvCleanupTool {

    static String clean(String input) {
        if (input == null || input.trim().isEmpty()) return ''
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
                int count = Math.max(headers.size(), row.size())
                printer.printRecord((0..<count).collect { i ->
                    row.size() > i ? row.get(i).trim().replaceAll(/\s+/, ' ') : ''
                })
            }
            printer.flush()
            printer.close()
        }
        writer.toString()
    }
}
