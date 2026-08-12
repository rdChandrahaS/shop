package com.shop.foodservice.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.shop.foodservice.exception.ResourceNotFoundException;
import com.shop.foodservice.model.Food;
import com.shop.foodservice.repo.FoodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodService {
	private final FoodRepository foodRepository;
	
	@Cacheable(value = "foods")
	public List<Food> getFoods() {
		return foodRepository.findAll();
	}
	
	@CacheEvict(value = "foods", allEntries = true)
	public Food addFood(Food food) {
		return foodRepository.save(food);
	}
	
	@Caching(evict = {
		@CacheEvict(value = "foods", allEntries = true),
		@CacheEvict(value = "food", key = "#id")
	})
	public Food updateFood(Integer id, Food updatedFood) {
		// 1. Fetch the existing food first
		Food existingFood = foodRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
		
		// 2. Update the fields
		if(updatedFood.getFoodDescription() != null) {
			existingFood.setFoodDescription(updatedFood.getFoodDescription());
		}
		if(updatedFood.getFoodName() != null) {
			existingFood.setFoodName(updatedFood.getFoodName());
		}
		if(updatedFood.getFoodPrice() != -1) {
			existingFood.setFoodPrice(updatedFood.getFoodPrice());
		}

		// 3. Save and return
		return foodRepository.save(existingFood);
	}
	
	@Caching(evict = {
		@CacheEvict(value = "foods", allEntries = true),
		@CacheEvict(value = "food", key = "#id")
	})
	public void deleteFood(Integer id) {
		if (!foodRepository.existsById(id)) {
			throw new ResourceNotFoundException("Food not found with ID: " + id);
		}
		foodRepository.deleteById(id);
	}
	
	@Cacheable(value = "food", key = "#id")
	public Food getFood(Integer id) {
		return foodRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
	}
}