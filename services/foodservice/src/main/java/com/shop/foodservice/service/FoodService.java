package com.shop.foodservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.foodservice.model.Food;
import com.shop.foodservice.repo.FoodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodService {
	private final FoodRepository foodRepo;
	
	public List<Food> getFoods() {
		return foodRepo.findAll();
	}

	public Food getFood(Integer foodId) {
		return foodRepo.findById(foodId)
				.orElseThrow(() -> new RuntimeException("Order not found with ID: " + foodId));
	}
	
	
	public boolean orderFood(Food food,int quantity) {
		return false;
	}
}
