package com.saga.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "http://localhost:8082")
public interface InventoryServiceClient {
    
    @PostMapping("/api/inventory/update")
    boolean updateInventory(@RequestParam("productId") String productId,
                             @RequestParam("quantity") Integer quantity);
    
    @PostMapping("/api/inventory/compensate")
    boolean compensateInventory(@RequestParam("productId") String productId,
                                 @RequestParam("quantity") Integer quantity);
}