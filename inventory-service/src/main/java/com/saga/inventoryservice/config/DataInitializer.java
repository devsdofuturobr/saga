package com.saga.inventoryservice.config;

import com.saga.inventoryservice.entity.Product;
import com.saga.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final ProductRepository productRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Inicializar produtos de exemplo
        if (productRepository.count() == 0) {
            Product product1 = new Product();
            product1.setId("PROD-001");
            product1.setName("Smartphone");
            product1.setDescription("Latest model smartphone with 128GB storage");
            product1.setPrice(699.99);
            product1.setStock(50);
            
            Product product2 = new Product();
            product2.setId("PROD-002");
            product2.setName("Laptop");
            product2.setDescription("High-performance laptop with 16GB RAM");
            product2.setPrice(1299.99);
            product2.setStock(30);
            
            Product product3 = new Product();
            product3.setId("PROD-003");
            product3.setName("Headphones");
            product3.setDescription("Wireless noise-cancelling headphones");
            product3.setPrice(299.99);
            product3.setStock(100);
            
            productRepository.save(product1);
            productRepository.save(product2);
            productRepository.save(product3);
            
            System.out.println("Sample products initialized successfully!");
        }
    }
}