package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/products")
@CrossOrigin(origins="http://localhost:5173")
/**
 * ProductController - controlador sencillo y seguro para productos.
 * - Devolvemos ResponseEntity<Object> para poder enviar Product o Map de error.
 * - @CrossOrigin permite peticiones desde el frontend en dev (ajusta el origen).
 */
public class ProductController {
        private final ProductRepository productRepository;
        private final ValidationService validationService;
        public ProductController(ProductRepository productRepository, ValidationService validationService) {
            this.productRepository = productRepository;
            this.validationService = validationService;
        }
        // GET /products -> lista de todos los productos
        @GetMapping
        public ResponseEntity<?> all(){
            return ResponseEntity.ok(productRepository.findAll());
        }
}

