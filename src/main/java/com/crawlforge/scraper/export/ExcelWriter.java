package com.crawlforge.scraper.export;

import com.crawlforge.scraper.core.exception.StorageException;
import com.crawlforge.scraper.core.model.ScrapedRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Excel export with Apache POI for BPO/data-entry style deliverables. */
public final class ExcelWriter implements DataWriter {

    @Override public String extension() { return "xlsx"; }

    @Override
    public void write(List<ScrapedRecord> records, String target) {
        Set<String> headers = new LinkedHashSet<>();
        records.forEach(record -> headers.addAll(record.toMap().keySet()));
        String[] columns = headers.toArray(String[]::new);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Scraped Data");
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowIndex = 1;
            for (ScrapedRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                var map = record.toMap();
                for (int i = 0; i < columns.length; i++) {
                    row.createCell(i).setCellValue(map.getOrDefault(columns[i], ""));
                }
            }
            for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
            Path path = Path.of(target);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (var out = Files.newOutputStream(path)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new StorageException("Could not write Excel file to " + target, e);
        }
    }
}
