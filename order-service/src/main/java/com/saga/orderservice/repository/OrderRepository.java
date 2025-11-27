package com.saga.orderservice.repository;

import com.saga.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(com.saga.orderservice.entity.OrderStatus status);
}