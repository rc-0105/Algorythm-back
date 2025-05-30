package com.example.algoritmos.algoritmos;

import org.apache.commons.csv.CSVRecord;
import smile.clustering.KMeans;

import java.util.*;

public class KMediasCompletar implements AlgoritmoCompletarDatos {
    @Override
    public List<String[]> completar(List<CSVRecord> registros) throws Exception {
        // Convertir los datos a double[][], solo columnas numéricas
        int numCols = registros.get(0).size();
        int numRows = registros.size();
        List<Integer> columnasNumericas = new ArrayList<>();
        // Detectar columnas numéricas
        for (int i = 0; i < numCols; i++) {
            boolean esNumerica = true;
            for (CSVRecord record : registros) {
                String val = record.get(i);
                if (val != null && !val.isEmpty()) {
                    try { Double.parseDouble(val); } catch (Exception e) { esNumerica = false; break; }
                }
            }
            if (esNumerica) columnasNumericas.add(i);
        }
        if (columnasNumericas.isEmpty()) throw new Exception("No hay columnas numéricas para K-Means");
        double[][] data = new double[numRows][columnasNumericas.size()];
        boolean[][] missing = new boolean[numRows][columnasNumericas.size()];
        for (int i = 0; i < numRows; i++) {
            CSVRecord record = registros.get(i);
            for (int j = 0; j < columnasNumericas.size(); j++) {
                String val = record.get(columnasNumericas.get(j));
                if (val == null || val.isEmpty()) {
                    data[i][j] = 0.0;
                    missing[i][j] = true;
                } else {
                    data[i][j] = Double.parseDouble(val);
                    missing[i][j] = false;
                }
            }
        }
        // Ejecutar K-Means (k = sqrt(n/2) o mínimo 2)
        int k = Math.max(2, (int)Math.sqrt(numRows/2));
        // Imputar valores faltantes numéricos con la media de la columna antes de KMeans
        double[] medias = new double[columnasNumericas.size()];
        for (int j = 0; j < columnasNumericas.size(); j++) {
            double suma = 0;
            int cuenta = 0;
            for (int i = 0; i < numRows; i++) {
                if (!missing[i][j]) {
                    suma += data[i][j];
                    cuenta++;
                }
            }
            medias[j] = cuenta > 0 ? suma / cuenta : 0.0;
        }
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < columnasNumericas.size(); j++) {
                if (missing[i][j]) {
                    data[i][j] = medias[j];
                }
            }
        }
        KMeans kmeans = KMeans.fit(data, k);
        // Precalcular modas por cluster para columnas categóricas
        List<Integer> columnasCategoricas = new ArrayList<>();
        for (int i = 0; i < numCols; i++) if (!columnasNumericas.contains(i)) columnasCategoricas.add(i);
        Map<Integer, Map<Integer, Map<String, Integer>>> clusterModa = new HashMap<>();
        for (int cluster = 0; cluster < k; cluster++) {
            clusterModa.put(cluster, new HashMap<>());
            for (int col : columnasCategoricas) {
                clusterModa.get(cluster).put(col, new HashMap<>());
            }
        }
        for (int i = 0; i < numRows; i++) {
            int cluster = kmeans.y[i];
            CSVRecord record = registros.get(i);
            for (int col : columnasCategoricas) {
                String val = record.get(col);
                if (val != null && !val.isEmpty()) {
                    clusterModa.get(cluster).get(col).put(val, clusterModa.get(cluster).get(col).getOrDefault(val, 0) + 1);
                }
            }
        }
        // Completar valores faltantes
        List<String[]> resultado = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            String[] row = new String[numCols];
            CSVRecord record = registros.get(i);
            int cluster = kmeans.y[i];
            for (int j = 0; j < numCols; j++) {
                if (columnasNumericas.contains(j)) {
                    int idx = columnasNumericas.indexOf(j);
                    if (missing[i][idx]) {
                        row[j] = String.format(Locale.US, "%.2f", kmeans.centroids[cluster][idx]);
                    } else {
                        row[j] = record.get(j);
                    }
                } else { // categórico
                    String val = record.get(j);
                    if (val == null || val.isEmpty()) {
                        // Moda del cluster
                        Map<String, Integer> cuenta = clusterModa.get(cluster).get(j);
                        String moda = null;
                        int max = -1;
                        for (Map.Entry<String, Integer> e : cuenta.entrySet()) {
                            if (e.getValue() > max) { moda = e.getKey(); max = e.getValue(); }
                        }
                        row[j] = moda != null ? moda : "";
                    } else {
                        row[j] = val;
                    }
                }
            }
            resultado.add(row);
        }
        // Escribir encabezados si existen
        if (!registros.isEmpty() && registros.get(0).getParser().getHeaderMap() != null) {
            String[] headers = registros.get(0).getParser().getHeaderMap().keySet().toArray(new String[0]);
            resultado.add(0, headers);
        }
        return resultado;
    }
}
