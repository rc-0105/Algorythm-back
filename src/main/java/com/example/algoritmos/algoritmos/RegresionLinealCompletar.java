package com.example.algoritmos.algoritmos;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import java.util.*;

public class RegresionLinealCompletar implements AlgoritmoCompletarDatos {
    @Override
    public List<String[]> completar(List<CSVRecord> registros) throws Exception {
        int numCols = registros.get(0).getParser().getHeaderMap().size();
        int numRows = registros.size();
        Map<String, Integer> headerMap = registros.get(0).getParser().getHeaderMap();
        List<String> headerList = new ArrayList<>(headerMap.keySet());

        // Filtrar filas vacías, comentarios y filas con columnas incorrectas
        List<CSVRecord> registrosLimpios = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            CSVRecord record = registros.get(i);
            boolean allEmpty = true;
            for (int j = 0; j < record.size(); j++) {
                String val = record.get(j);
                if (val != null && !val.trim().isEmpty()) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) continue;
            if (record.size() > 0 && record.get(0).trim().startsWith("#")) continue;
            if (record.size() != numCols) {
                throw new Exception("Fila " + (i+2) + " tiene "+record.size()+" columnas, pero el encabezado tiene "+numCols+". Corrige el CSV para que todas las filas tengan el mismo número de columnas.");
            }
            registrosLimpios.add(record);
        }
        if (registrosLimpios.size() < 2) {
            throw new Exception("No hay suficientes filas válidas para realizar regresión lineal tras filtrar vacías/comentarios.");
        }
        numRows = registrosLimpios.size();
        // Detectar columnas numéricas
        List<Integer> columnasNumericas = new ArrayList<>();
        for (int j = 0; j < numCols; j++) {
            boolean esNumerica = true;
            int countValid = 0;
            for (int i = 0; i < numRows; i++) {
                String val = null;
                if (j < headerList.size()) {
                    String colName = headerList.get(j);
                    if (registrosLimpios.get(i).isMapped(colName)) {
                        val = registrosLimpios.get(i).get(colName);
                    }
                }
                if (val != null && !val.isEmpty()) {
                    try { Double.parseDouble(val); countValid++; } catch (Exception e) { esNumerica = false; break; }
                }
            }
            if (esNumerica && countValid >= 2) columnasNumericas.add(j);
        }
        if (columnasNumericas.size() < 2) throw new Exception("Se requieren al menos dos columnas numéricas para regresión lineal");
        // Preparar matriz de datos y missing
        double[][] data = new double[numRows][numCols];
        boolean[][] missing = new boolean[numRows][numCols];
        for (int i = 0; i < numRows; i++) {
            CSVRecord record = registrosLimpios.get(i);
            for (int j = 0; j < numCols; j++) {
                String val = null;
                if (j < headerList.size()) {
                    String colName = headerList.get(j);
                    if (record.isMapped(colName)) {
                        val = record.get(colName);
                    }
                }
                if (val == null || val.isEmpty()) {
                    data[i][j] = 0.0;
                    missing[i][j] = true;
                } else {
                    try {
                        data[i][j] = Double.parseDouble(val);
                        missing[i][j] = false;
                    } catch (Exception e) {
                        data[i][j] = 0.0;
                        missing[i][j] = true;
                    }
                }
            }
        }
        // Imputar valores faltantes usando OLSMultipleLinearRegression
        boolean huboCambios;
        int maxIter = 10; // Evita bucles infinitos
        int iter = 0;
        do {
            huboCambios = false;
            for (int targetIdx = 0; targetIdx < columnasNumericas.size(); targetIdx++) {
                int targetCol = columnasNumericas.get(targetIdx);
                List<Integer> predCols = new ArrayList<>();
                for (int idx = 0; idx < columnasNumericas.size(); idx++) {
                    if (idx != targetIdx) predCols.add(columnasNumericas.get(idx));
                }
                int numPredictores = predCols.size();
                List<double[]> xList = new ArrayList<>();
                List<Double> yList = new ArrayList<>();
                for (int i = 0; i < numRows; i++) {
                    if (!missing[i][targetCol]) {
                        double[] x = new double[numPredictores];
                        boolean skip = false;
                        for (int k = 0; k < numPredictores; k++) {
                            int col = predCols.get(k);
                            if (col >= numCols || missing[i][col]) { skip = true; break; }
                            x[k] = data[i][col];
                        }
                        if (skip) continue;
                        yList.add(data[i][targetCol]);
                        xList.add(x);
                    }
                }
                if (xList.size() <= numPredictores) continue;
                double[][] X = xList.toArray(new double[0][]);
                double[] y = yList.stream().mapToDouble(Double::doubleValue).toArray();
                OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
                ols.setNoIntercept(false);
                ols.newSampleData(y, X);
                double[] beta = ols.estimateRegressionParameters();
                for (int i = 0; i < numRows; i++) {
                    if (missing[i][targetCol]) {
                        double[] x = new double[numPredictores];
                        boolean skip = false;
                        for (int k = 0; k < numPredictores; k++) {
                            int col = predCols.get(k);
                            if (col >= numCols || missing[i][col]) { skip = true; break; }
                            x[k] = data[i][col];
                        }
                        if (skip) continue;
                        double pred = beta[0]; // intercepto
                        for (int k = 0; k < numPredictores; k++) {
                            pred += beta[k+1] * x[k];
                        }
                        data[i][targetCol] = pred;
                        missing[i][targetCol] = false;
                        huboCambios = true;
                    }
                }
            }
            iter++;
        } while (huboCambios && iter < maxIter);
        // Segunda pasada: rellenar con la media de la columna si aún quedan vacíos
        for (int j : columnasNumericas) {
            double suma = 0;
            int cuenta = 0;
            for (int i = 0; i < numRows; i++) {
                if (!missing[i][j]) {
                    suma += data[i][j];
                    cuenta++;
                }
            }
            double media = cuenta > 0 ? suma / cuenta : 0.0;
            for (int i = 0; i < numRows; i++) {
                if (missing[i][j]) {
                    data[i][j] = media;
                    missing[i][j] = false;
                }
            }
        }
        // Construir resultado final
        List<String[]> resultado = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            String[] row = new String[numCols];
            CSVRecord record = registrosLimpios.get(i);
            for (int j = 0; j < numCols; j++) {
                if (columnasNumericas.contains(j)) {
                    if (missing[i][j]) {
                        row[j] = "";
                    } else {
                        row[j] = String.format(Locale.US, "%.2f", data[i][j]);
                    }
                } else {
                    if (j < headerList.size()) {
                        String colName = headerList.get(j);
                        if (record.isMapped(colName)) {
                            row[j] = record.get(colName);
                        } else {
                            row[j] = "";
                        }
                    } else {
                        row[j] = "";
                    }
                }
            }
            resultado.add(row);
        }
        if (!registros.isEmpty() && registros.get(0).getParser().getHeaderMap() != null) {
            String[] headers = registros.get(0).getParser().getHeaderMap().keySet().toArray(new String[0]);
            resultado.add(0, headers);
        }
        return resultado;
    }
}
