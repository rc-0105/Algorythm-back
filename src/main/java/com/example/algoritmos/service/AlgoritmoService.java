package com.example.algoritmos.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface AlgoritmoService {
    Resource completarDatos(MultipartFile file, String tipoAlgoritmo) throws Exception;
}
