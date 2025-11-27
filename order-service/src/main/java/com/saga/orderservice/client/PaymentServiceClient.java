package com.saga.orderservice.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service", url = "http://localhost:8081")
public interface PaymentServiceClient {
    
    @PostMapping("/api/payments/process")
    boolean processPayment(@RequestParam("orderId") Long orderId,
                          @RequestParam("customerId") String customerId,
                          @RequestParam("amount") BigDecimal amount);
    
    @PostMapping("/api/payments/refund")
    boolean refundPayment(@RequestParam("orderId") Long orderId);
}