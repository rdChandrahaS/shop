package com.shop.foodservice.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.shop.foodservice.exception.ResourceNotFoundException;
import com.shop.foodservice.model.Food;
import com.shop.foodservice.repo.FoodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodService {
	private final FoodRepository foodRepository;
	
	@Cacheable(value = "foods_menu")
	public List<Food> getFoods() {
		return foodRepository.findAll();
	}
	
	@CacheEvict(value = "foods_menu", allEntries = true)
    public Food addFood(Food food) {
        return foodRepository.save(food);
    }
	
	@CacheEvict(value = "foods_menu", allEntries = true)
    public Food updateFood(Integer id, Food updatedFood) {
        // 1. Fetch the existing food first
        Food existingFood = foodRepository.findById(id)
        		.orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
        
        // 2. Update the fields (assuming your Food model has these setters)
        if(updatedFood.getFoodDecription() != null) {
        	existingFood.setFoodDecription(updatedFood.getFoodDecription());
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
    
    @CacheEvict(value = "foods_menu", allEntries = true)
    public void deleteFood(Integer id) {
    	if (!foodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Food not found with ID: " + id);
        }
        foodRepository.deleteById(id);
    }
    
    @Cacheable(value = "food_item", key = "#id")
    public Food getFood(Integer id) {
        return foodRepository.findById(id)
        		.orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
    }
}
