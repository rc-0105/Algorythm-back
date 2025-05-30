package com.example.algoritmos.algoritmos;

import org.apache.commons.csv.CSVRecord;
import java.util.List;

public interface AlgoritmoCompletarDatos {
    List<String[]> completar(List<CSVRecord> registros) throws Exception;
}
