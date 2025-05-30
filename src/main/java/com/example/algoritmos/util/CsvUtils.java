package com.example.algoritmos.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.List;

public class CsvUtils {
    public static List<CSVRecord> readCsv(InputStream inputStream) throws IOException {
        Reader reader = new InputStreamReader(inputStream);
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        return parser.getRecords();
    }

    public static void writeCsv(List<String[]> data, OutputStream outputStream) throws IOException {
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(outputStream), CSVFormat.DEFAULT);
        for (String[] row : data) {
            printer.printRecord((Object[]) row);
        }
        printer.flush();
    }
}
