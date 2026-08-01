package com.shop.orderingservice.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.shop.orderingservice.model.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    
    Optional<Order> findById(String id);
    Page<Order> findByCustomer_CustomerId(String customerId, Pageable pageable);
}