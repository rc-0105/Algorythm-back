package com.example.algoritmos.algoritmos;

import org.apache.commons.csv.CSVRecord;
import java.util.*;

public class KModesCompletar implements AlgoritmoCompletarDatos {
    @Override
    public List<String[]> completar(List<CSVRecord> registros) throws Exception {
        int numCols = registros.get(0).getParser().getHeaderMap().size();
        int numRows = registros.size();
        // Detectar columnas categóricas (no numéricas)
        List<Integer> columnasCategoricas = new ArrayList<>();
        for (int j = 0; j < numCols; j++) {
            boolean esNumerica = true;
            for (int i = 0; i < numRows; i++) {
                String val = (j < registros.get(i).size()) ? registros.get(i).get(j) : null;
                if (val != null && !val.isEmpty()) {
                    try { Double.parseDouble(val); } catch (Exception e) { esNumerica = false; break; }
                }
            }
            if (!esNumerica) columnasCategoricas.add(j);
        }
        if (columnasCategoricas.isEmpty()) throw new Exception("No hay columnas categóricas para K-Modes");
        // Convertir a matriz de strings
        String[][] data = new String[numRows][numCols];
        boolean[][] missing = new boolean[numRows][numCols];
        for (int i = 0; i < numRows; i++) {
            CSVRecord record = registros.get(i);
            for (int j = 0; j < numCols; j++) {
                String val = (j < record.size()) ? record.get(j) : null;
                if (val == null || val.isEmpty()) {
                    data[i][j] = null;
                    missing[i][j] = true;
                } else {
                    data[i][j] = val;
                    missing[i][j] = false;
                }
            }
        }
        // Validar que cada fila tiene exactamente el mismo número de columnas que el encabezado
        for (int i = 0; i < numRows; i++) {
            if (registros.get(i).size() != numCols) {
                throw new Exception("Fila " + (i+2) + " tiene "+registros.get(i).size()+" columnas, pero el encabezado tiene "+numCols+". Corrige el CSV para que todas las filas tengan el mismo número de columnas.");
            }
        }
        // Elegir k clusters (heurística: sqrt(n/2) o mínimo 2)
        int k = Math.max(2, (int)Math.sqrt(numRows/2));
        // Inicializar modos aleatoriamente
        List<String[]> modos = new ArrayList<>();
        Random rand = new Random(42);
        Set<Integer> usados = new HashSet<>();
        while (modos.size() < k) {
            int idx = rand.nextInt(numRows);
            if (!usados.contains(idx)) {
                String[] modo = new String[columnasCategoricas.size()];
                for (int j = 0; j < columnasCategoricas.size(); j++) {
                    int col = columnasCategoricas.get(j);
                    modo[j] = data[idx][col];
                }
                modos.add(modo);
                usados.add(idx);
            }
        }
        // Asignar clusters y actualizar modos
        int[] asignacion = new int[numRows];
        boolean cambio;
        int maxIter = 10;
        for (int iter = 0; iter < maxIter; iter++) {
            cambio = false;
            // Asignar cada fila al modo más cercano (menos diferencias)
            for (int i = 0; i < numRows; i++) {
                int mejor = 0;
                int mejorDist = Integer.MAX_VALUE;
                for (int m = 0; m < k; m++) {
                    int dist = 0;
                    for (int j = 0; j < columnasCategoricas.size(); j++) {
                        int col = columnasCategoricas.get(j);
                        String v1 = data[i][col];
                        String v2 = modos.get(m)[j];
                        if (v1 == null || v2 == null || !v1.equals(v2)) dist++;
                    }
                    if (dist < mejorDist) {
                        mejorDist = dist;
                        mejor = m;
                    }
                }
                if (asignacion[i] != mejor) {
                    cambio = true;
                    asignacion[i] = mejor;
                }
            }
            // Actualizar modos (moda por columna categórica en cada cluster)
            for (int m = 0; m < k; m++) {
                List<String[]> enCluster = new ArrayList<>();
                for (int i = 0; i < numRows; i++) if (asignacion[i] == m) enCluster.add(data[i]);
                for (int j = 0; j < columnasCategoricas.size(); j++) {
                    int col = columnasCategoricas.get(j);
                    Map<String, Integer> cuenta = new HashMap<>();
                    for (String[] fila : enCluster) {
                        String val = fila[col];
                        if (val != null) cuenta.put(val, cuenta.getOrDefault(val, 0) + 1);
                    }
                    String moda = null;
                    int max = -1;
                    for (Map.Entry<String, Integer> e : cuenta.entrySet()) {
                        if (e.getValue() > max) { moda = e.getKey(); max = e.getValue(); }
                    }
                    modos.get(m)[j] = moda;
                }
            }
            if (!cambio) break;
        }
        // Imputar valores faltantes categóricos con la moda del cluster asignado
        for (int i = 0; i < numRows; i++) {
            int cluster = asignacion[i];
            for (int j = 0; j < columnasCategoricas.size(); j++) {
                int col = columnasCategoricas.get(j);
                if (missing[i][col]) {
                    data[i][col] = modos.get(cluster)[j];
                    missing[i][col] = false;
                }
            }
        }
        // Construir resultado final
        List<String[]> resultado = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            String[] row = new String[numCols];
            CSVRecord record = registros.get(i);
            for (int j = 0; j < numCols; j++) {
                if (columnasCategoricas.contains(j)) {
                    row[j] = (data[i][j] != null) ? data[i][j] : "";
                } else {
                    row[j] = (j < record.size()) ? record.get(j) : "";
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
