package com.example.algoritmos.controller;

import com.example.algoritmos.service.AlgoritmoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/algoritmo")
public class AlgoritmoController {
    @Autowired
    private AlgoritmoService algoritmoService;

    // Recibe archivo y tipo de algoritmo
    @PostMapping(value = "/completar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completarDatos(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") String tipoAlgoritmo) {
        try {
            var result = algoritmoService.completarDatos(file, tipoAlgoritmo);
            // Convertir el recurso a String y luego a JSON (lista de listas)
            java.io.InputStream is = ((org.springframework.core.io.ByteArrayResource) result).getInputStream();
            java.util.List<java.util.List<String>> json = new java.util.ArrayList<>();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Manejo robusto de comas y columnas vacías
                    String[] parts = line.split(",", -1);
                    java.util.List<String> row = java.util.Arrays.asList(parts);
                    json.add(row);
                }
            }
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
