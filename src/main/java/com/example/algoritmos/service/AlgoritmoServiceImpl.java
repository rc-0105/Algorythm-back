package com.example.algoritmos.service;

import com.example.algoritmos.algoritmos.AlgoritmoCompletarDatos;
import com.example.algoritmos.algoritmos.ArbolDecisionCompletar;
import com.example.algoritmos.algoritmos.KMediasCompletar;
import com.example.algoritmos.algoritmos.KModesCompletar;
import com.example.algoritmos.algoritmos.RegresionLinealCompletar;
import com.example.algoritmos.util.CsvUtils;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class AlgoritmoServiceImpl implements AlgoritmoService {
    @Override
    public Resource completarDatos(MultipartFile file, String tipoAlgoritmo) throws Exception {
        List<CSVRecord> registros = CsvUtils.readCsv(file.getInputStream());
        AlgoritmoCompletarDatos algoritmo;
        switch (tipoAlgoritmo.toLowerCase()) {
            case "arbol":
            case "arboles":
            case "arboles_de_decision":
            case "arbolesdedecision":
            case "arboles-de-decision":
            case "arbolesdecision":
            case "arbol-decision":
                algoritmo = new ArbolDecisionCompletar();
                break;
            case "kmeans":
            case "k-medias":
            case "kmedias":
            case "k-means":
                algoritmo = new KMediasCompletar();
                break;
            case "regresion":
            case "regresionlineal":
            case "regresion-lineal":
            case "lineal":
                algoritmo = new RegresionLinealCompletar();
                break;
            case "kmodes":
            case "k-modes":
            case "kmodos":
                algoritmo = new KModesCompletar();
                break;
            // Otros algoritmos se agregarán aquí
            default:
                throw new IllegalArgumentException("Tipo de algoritmo no soportado: " + tipoAlgoritmo);
        }
        List<String[]> resultado = algoritmo.completar(registros);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvUtils.writeCsv(resultado, baos);
        return new ByteArrayResource(baos.toByteArray());
    }
}
