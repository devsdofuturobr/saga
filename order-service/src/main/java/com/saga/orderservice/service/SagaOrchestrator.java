package com.saga.orderservice.service;

import org.springframework.stereotype.Service;

import com.saga.orderservice.client.InventoryServiceClient;
import com.saga.orderservice.client.PaymentServiceClient;
import com.saga.orderservice.entity.Order;
import com.saga.orderservice.entity.OrderStatus;
import com.saga.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {
    
    private final PaymentServiceClient paymentServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderRepository orderRepository;
    
    public void startOrderSaga(Order order) {
        log.info("Starting SAGA for order: {}", order.getId());
        
        try {
            // Passo 1: Processar pagamento
            log.info("Step 1: Processing payment for order {}", order.getId());
            updateOrderStatus(order.getId(), OrderStatus.PAYMENT_PROCESSING);
            
            boolean paymentProcessed = paymentServiceClient.processPayment(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount()
            );
            
            if (!paymentProcessed) {
                log.error("Payment failed for order {}", order.getId());
                updateOrderStatus(order.getId(), OrderStatus.PAYMENT_FAILED);
                return;
            }
            
            updateOrderStatus(order.getId(), OrderStatus.PAYMENT_COMPLETED);
            log.info("Payment completed for order {}", order.getId());
            
            // Passo 2: Atualizar inventário
            log.info("Step 2: Updating inventory for order {}", order.getId());
            updateOrderStatus(order.getId(), OrderStatus.INVENTORY_PROCESSING);
            
            boolean inventoryUpdated = inventoryServiceClient.updateInventory(
                order.getProductId(),
                order.getQuantity()
            );
            
            if (!inventoryUpdated) {
                log.error("Inventory update failed for order {}", order.getId());
                // Compensar: reembolsar pagamento
                paymentServiceClient.refundPayment(order.getId());
                updateOrderStatus(order.getId(), OrderStatus.INVENTORY_FAILED);
                return;
            }
            
            updateOrderStatus(order.getId(), OrderStatus.INVENTORY_COMPLETED);
            log.info("Inventory updated for order {}", order.getId());
            
            // Sucesso: completar ordem
            updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
            log.info("SAGA completed successfully for order {}", order.getId());
            
        } catch (Exception e) {
            log.error("Error in SAGA for order {}: {}", order.getId(), e.getMessage());
            handleSagaFailure(order);
        }
    }
    
    private void handleSagaFailure(Order order) {
        log.info("Handling SAGA failure for order {}", order.getId());
        
        try {
            // Compensar pagamento se necessário
            if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED || 
                order.getStatus() == OrderStatus.INVENTORY_PROCESSING ||
                order.getStatus() == OrderStatus.INVENTORY_FAILED) {
                
                log.info("Refunding payment for order {}", order.getId());
                paymentServiceClient.refundPayment(order.getId());
            }
            
            // Cancelar ordem
            updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
            log.info("Order {} cancelled due to SAGA failure", order.getId());
            
        } catch (Exception e) {
            log.error("Error during SAGA compensation for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void updateOrderStatus(Long orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
    }
}
