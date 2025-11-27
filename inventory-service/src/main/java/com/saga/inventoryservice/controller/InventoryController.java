package com.saga.inventoryservice.controller;

import com.saga.inventoryservice.entity.Product;
import com.saga.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    @PostMapping("/update")
    public ResponseEntity<Boolean> updateInventory(@RequestParam("productId") String productId,
                                                    @RequestParam("quantity") Integer quantity) {
        log.info("Received inventory update request for product: {}, quantity: {}", productId, quantity);
        boolean result = inventoryService.updateInventory(productId, quantity);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/compensate")
    public ResponseEntity<Boolean> compensateInventory(@RequestParam("productId") String productId,
                                                      @RequestParam("quantity") Integer quantity) {
        log.info("Received inventory compensation request for product: {}, quantity: {}", productId, quantity);
        boolean result = inventoryService.compensateInventory(productId, quantity);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = inventoryService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/available")
    public ResponseEntity<List<Product>> getAvailableProducts() {
        List<Product> products = inventoryService.getAvailableProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable String productId) {
        Optional<Product> product = inventoryService.getProductById(productId);
        return product.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product createdProduct = inventoryService.createProduct(product);
        return ResponseEntity.ok(createdProduct);
    }
    
    @PutMapping("/products/{productId}/stock")
    public ResponseEntity<Product> updateProductStock(@PathVariable String productId,
                                                      @RequestParam Integer stock) {
        Product updatedProduct = inventoryService.updateProductStock(productId, stock);
        return ResponseEntity.ok(updatedProduct);
    }
}