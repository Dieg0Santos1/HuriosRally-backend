package com.hurios.huriosbackend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageUploadController {

@PostMapping("/upload")
public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {

    if (file.isEmpty()) {

        return ResponseEntity.badRequest().body(
                Map.of("error", "El archivo está vacío")
        );
    }

    return ResponseEntity.ok().build();
}


}