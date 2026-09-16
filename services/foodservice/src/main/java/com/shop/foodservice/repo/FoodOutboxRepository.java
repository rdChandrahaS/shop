package com.shop.foodservice.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.foodservice.model.FoodOutboxEvent;

public interface FoodOutboxRepository extends JpaRepository<FoodOutboxEvent, Long> {

    List<FoodOutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
