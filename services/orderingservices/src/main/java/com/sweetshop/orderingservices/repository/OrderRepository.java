package com.sweetshop.orderingservices.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sweetshop.orderingservices.model.Order;

public interface OrderRepository extends JpaRepository<Order , Long>{

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    
}
