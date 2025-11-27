package com.saga.inventoryservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.saga.inventoryservice.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    List<Product> findByStockGreaterThan(Integer stock);
    
    @Query("SELECT p FROM Product p WHERE p.stock >= :minStock")
    List<Product> findProductsWithMinimumStock(@Param("minStock") Integer minStock);
}