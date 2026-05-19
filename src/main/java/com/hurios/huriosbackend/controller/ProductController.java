package com.hurios.huriosbackend.controller;

import com.hurios.huriosbackend.entity.Product;
import com.hurios.huriosbackend.repository.ProductRepository;
import com.hurios.huriosbackend.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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
        // GET /products -> Lista de todos los productos
        @GetMapping
        public ResponseEntity<?> all(){
            return ResponseEntity.ok(productRepository.findAll());
        }
        //GET /products/{id} -> Muestra los datos del producto seleccionado por su ID
        @GetMapping("/{id}")
        public ResponseEntity<Object> getProducto(@PathVariable Long id) {
            Optional<Product> maybe = productRepository.findById(id);
            if (maybe.isPresent()) {
                // Devolvemos 200 OK con el producto
                return ResponseEntity.ok(maybe.get());
            }
            // Si no existe, devolvemos 404 con un objeto JSON explicativo
            return ResponseEntity.status(404).body(
                    Map.of("error", "Producto no encontrado")
            );
        }

        @PutMapping("/{id}/add-stock")
        public ResponseEntity<?> addStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
            Optional<Product> maybe = productRepository.findById(id);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(
                        Map.of("error", "Producto no encontrado")
                );
            }

            Integer quantity = body.get("quantity");
            if (quantity == null || quantity <= 0) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "La cantidad debe ser mayor a 0")
                );
            }

            Product product = maybe.get();
            int currentStock = product.getStock() != null ? product.getStock() : 0;
            product.setStock(currentStock + quantity);
            productRepository.save(product);

            return ResponseEntity.ok(Map.of(
                    "message", "Stock actualizado correctamente",
                    "newStock", product.getStock()
            ));
        }
}

