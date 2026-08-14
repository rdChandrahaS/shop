package com.shop.foodservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.foodservice.model.Food;

public interface FoodRepository extends JpaRepository<Food, Long>{

}
