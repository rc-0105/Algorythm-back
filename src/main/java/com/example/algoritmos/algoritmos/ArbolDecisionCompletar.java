package com.example.algoritmos.algoritmos;

import org.apache.commons.csv.CSVRecord;
import weka.classifiers.trees.J48;
import weka.core.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArbolDecisionCompletar implements AlgoritmoCompletarDatos {
    @Override
    public List<String[]> completar(List<CSVRecord> registros) throws Exception {
        // Detectar valores únicos por columna para crear atributos nominales
        int numCols = registros.get(0).size();
        List<Set<String>> valoresUnicos = new ArrayList<>();
        for (int i = 0; i < numCols; i++) valoresUnicos.add(new HashSet<>());
        for (CSVRecord record : registros) {
            for (int i = 0; i < numCols; i++) {
                String val = record.get(i);
                if (val != null && !val.isEmpty()) valoresUnicos.get(i).add(val);
            }
        }
        // Crear atributos nominales
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < numCols; i++) {
            ArrayList<String> valores = new ArrayList<>(valoresUnicos.get(i));
            attributes.add(new Attribute("col" + i, valores));
        }
        Instances data = new Instances("dataset", attributes, registros.size());
        data.setClassIndex(numCols - 1); // Suponemos que la última columna es la clase

        // Cargar datos y marcar los faltantes
        for (CSVRecord record : registros) {
            double[] vals = new double[numCols];
            for (int i = 0; i < numCols; i++) {
                String val = record.get(i);
                if (val == null || val.isEmpty()) {
                    vals[i] = Utils.missingValue();
                } else {
                    vals[i] = attributes.get(i).indexOfValue(val);
                }
            }
            data.add(new DenseInstance(1.0, vals));
        }

        // Entrenar árbol de decisión
        J48 tree = new J48();
        tree.buildClassifier(data);

        // Completar los valores faltantes usando el árbol
        List<String[]> resultado = new ArrayList<>();
        Enumeration<Instance> en = data.enumerateInstances();
        while (en.hasMoreElements()) {
            Instance inst = en.nextElement();
            String[] row = new String[numCols];
            for (int i = 0; i < numCols; i++) {
                if (inst.isMissing(i)) {
                    double pred = tree.classifyInstance(inst);
                    int predIdx = (int) pred;
                    if (predIdx >= 0 && predIdx < attributes.get(i).numValues()) {
                        row[i] = attributes.get(i).value(predIdx);
                    } else {
                        row[i] = ""; // Valor fuera de rango, dejar vacío
                    }
                } else {
                    int valIdx = (int) inst.value(i);
                    if (valIdx >= 0 && valIdx < attributes.get(i).numValues()) {
                        row[i] = attributes.get(i).value(valIdx);
                    } else {
                        row[i] = "";
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
