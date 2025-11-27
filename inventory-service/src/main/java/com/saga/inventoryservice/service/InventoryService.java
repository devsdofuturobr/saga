package com.saga.inventoryservice.service;

import com.saga.inventoryservice.entity.Product;
import com.saga.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final ProductRepository productRepository;
    
    @Transactional
    public boolean updateInventory(String productId, Integer quantity) {
        log.info("Updating inventory for product: {}, quantity: {}", productId, quantity);
        
        try {
            Optional<Product> productOptional = productRepository.findById(productId);
            
            if (!productOptional.isPresent()) {
                log.error("Product not found: {}", productId);
                return false;
            }
            
            Product product = productOptional.get();
            
            // Verificar se há estoque suficiente
            if (product.getStock() < quantity) {
                log.error("Insufficient stock for product: {}. Available: {}, Requested: {}", 
                         productId, product.getStock(), quantity);
                return false;
            }
            
            // Atualizar estoque
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            
            log.info("Inventory updated successfully for product: {}. New stock: {}", 
                    productId, product.getStock());
            return true;
            
        } catch (Exception e) {
            log.error("Error updating inventory for product {}: {}", productId, e.getMessage());
            return false;
        }
    }
    
    @Transactional
    public boolean compensateInventory(String productId, Integer quantity) {
        log.info("Compensating inventory for product: {}, quantity: {}", productId, quantity);
        
        try {
            Optional<Product> productOptional = productRepository.findById(productId);
            
            if (!productOptional.isPresent()) {
                log.error("Product not found: {}", productId);
                return false;
            }
            
            Product product = productOptional.get();
            
            // Devolver estoque
            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
            
            log.info("Inventory compensated successfully for product: {}. New stock: {}", 
                    productId, product.getStock());
            return true;
            
        } catch (Exception e) {
            log.error("Error compensating inventory for product {}: {}", productId, e.getMessage());
            return false;
        }
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public List<Product> getAvailableProducts() {
        return productRepository.findByStockGreaterThan(0);
    }
    
    public Optional<Product> getProductById(String productId) {
        return productRepository.findById(productId);
    }
    
    @Transactional
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getId());
        return productRepository.save(product);
    }
    
    @Transactional
    public Product updateProductStock(String productId, Integer newStock) {
        log.info("Updating product stock: {}, new stock: {}", productId, newStock);
        
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            product.setStock(newStock);
            return productRepository.save(product);
        }
        
        throw new RuntimeException("Product not found: " + productId);
    }
}