package com.hurios.huriosbackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageUploadController {

    // Directorio donde se guardarán las imágenes
    private static final String UPLOAD_DIR = "uploads/products/";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validar que el archivo no esté vacío
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "El archivo está vacío")
                );
            }

            // Validar tipo de archivo (solo imágenes)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "El archivo debe ser una imagen")
                );
            }

            // Crear directorio si no existe
            File uploadPath = new File(UPLOAD_DIR);
            if (!uploadPath.exists()) {
                uploadPath.mkdirs();
            }

            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Guardar el archivo
            Path destinationPath = Paths.get(UPLOAD_DIR + uniqueFilename);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // Retornar la URL pública del archivo
            String imageUrl = "/uploads/products/" + uniqueFilename;

            return ResponseEntity.ok(Map.of(
                "imageUrl", imageUrl,
                "filename", uniqueFilename,
                "message", "Imagen subida correctamente"
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(
                Map.of("error", "Error al subir la imagen: " + e.getMessage())
            );
        }
    }
}
