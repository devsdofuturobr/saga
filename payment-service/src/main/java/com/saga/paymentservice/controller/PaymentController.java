package com.saga.paymentservice.controller;

import com.saga.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<Boolean> processPayment(@RequestParam("orderId") Long orderId,
                                                  @RequestParam("customerId") String customerId,
                                                  @RequestParam("amount") BigDecimal amount) {
        log.info("Received payment request for order: {}, customer: {}, amount: {}", orderId, customerId, amount);
        boolean result = paymentService.processPayment(orderId, customerId, amount);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/refund")
    public ResponseEntity<Boolean> refundPayment(@RequestParam("orderId") Long orderId) {
        log.info("Received refund request for order: {}", orderId);
        boolean result = paymentService.refundPayment(orderId);
        return ResponseEntity.ok(result);
    }
}