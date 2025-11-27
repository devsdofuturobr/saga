package com.saga.paymentservice.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saga.paymentservice.entity.Payment;
import com.saga.paymentservice.entity.PaymentStatus;
import com.saga.paymentservice.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    
    @Transactional
    public boolean processPayment(Long orderId, String customerId, BigDecimal amount) {
        log.info("Processing payment for order: {}, customer: {}, amount: {}", orderId, customerId, amount);
        
        try {
            // Verificar se já existe pagamento para esta ordem
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
            if (existingPayment.isPresent()) {
                log.warn("Payment already exists for order: {}", orderId);
                return existingPayment.get().getStatus() == PaymentStatus.COMPLETED;
            }
            
            // Criar novo pagamento
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setCustomerId(customerId);
            payment.setAmount(amount);
            payment.setStatus(PaymentStatus.PENDING);
            
            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment created with ID: {}", savedPayment.getId());
            
            // Simular processamento de pagamento
            boolean paymentSuccessful = simulatePaymentProcessing();
            
            if (paymentSuccessful) {
                savedPayment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(savedPayment);
                log.info("Payment completed for order: {}", orderId);
                return true;
            } else {
                savedPayment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(savedPayment);
                log.error("Payment failed for order: {}", orderId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error processing payment for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }
    
    @Transactional
    public boolean refundPayment(Long orderId) {
        log.info("Processing refund for order: {}", orderId);
        
        try {
            Optional<Payment> paymentOptional = paymentRepository.findByOrderId(orderId);
            
            if (!paymentOptional.isPresent()) {
                log.warn("No payment found for order: {}", orderId);
                return false;
            }
            
            Payment payment = paymentOptional.get();
            
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                log.warn("Cannot refund payment for order {} - status is: {}", orderId, payment.getStatus());
                return false;
            }
            
            // Simular reembolso
            boolean refundSuccessful = simulateRefundProcessing();
            
            if (refundSuccessful) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                log.info("Refund completed for order: {}", orderId);
                return true;
            } else {
                log.error("Refund failed for order: {}", orderId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error processing refund for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }
    
    private boolean simulatePaymentProcessing() {
        // Simular 90% de sucesso no pagamento
        return Math.random() > 0.1;
    }
    
    private boolean simulateRefundProcessing() {
        // Simular 95% de sucesso no reembolso
        return Math.random() > 0.05;
    }
}
