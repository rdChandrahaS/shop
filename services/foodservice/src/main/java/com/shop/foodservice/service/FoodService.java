package com.shop.foodservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.shop.foodservice.dto.FoodRequest;
import com.shop.foodservice.dto.FoodResponse;
import com.shop.foodservice.exception.ResourceNotFoundException;
import com.shop.foodservice.model.Food;
import com.shop.foodservice.protobuf.FoodResponseProto;
import com.shop.foodservice.publisher.FoodEventPublisher;
import com.shop.foodservice.repo.FoodRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodService {
	
	private final FoodRepository foodRepository;
	private final FoodEventPublisher foodEventPublisher;
	
	@Cacheable(value = "foods")
	public List<FoodResponse> getFoods() {
		try {
			log.info("Service : Get all Foods Details");
			List<Food> foods = foodRepository.findAll();
			return foods.stream().map(food -> {
				FoodResponse response = new FoodResponse();
		        response.setFoodId(food.getFoodId());
		        response.setFoodName(food.getFoodName());
		        response.setFoodDescription(food.getFoodDescription());
		        response.setFoodPrice(food.getFoodPrice());
		        return response;
			}).collect(Collectors.toList());
		}catch(Exception e) {
			log.error("Failed to extract foods from the database", e);
			throw new RuntimeException("Database error occurred while fetching the food menu", e);
		}
	}
	
	@CacheEvict(value = "foods", allEntries = true)
	public ResponseEntity<FoodResponse> addFood(FoodRequest request) {
		log.info("Service : Add new food into database : {}", request);
		Food food = new Food();
	    food.setFoodName(request.getFoodName());
	    food.setFoodDescription(request.getFoodDescription());
	    food.setFoodPrice(request.getFoodPrice());
	    
	    try {
	    	Food savedFood = foodRepository.save(food);
	    	FoodResponse response = new FoodResponse(
	    			savedFood.getFoodId(),
	    			savedFood.getFoodName(),
	    			savedFood.getFoodDescription(),
	    			savedFood.getFoodPrice()
	    	);
	    	
	    	FoodResponseProto proto = FoodResponseProto.newBuilder()
	    	        .setFoodId(response.getFoodId())
	    	        .setFoodName(response.getFoodName())
	    	        .setFoodDescription(response.getFoodDescription())
	    	        .setFoodPrice(response.getFoodPrice().toString())
	    	        .build();
	    	
	    	log.info("Broadcasting food update to RabbitMQ: {}", response);
	    	foodEventPublisher.broadcastFoodUpdate(proto.toByteArray());
	        
	    	log.info("Saved new food into database : {}", response);
	    	return ResponseEntity.status(HttpStatus.CREATED).body(response);	    
	    
	    }catch(Exception e) {
	    	log.error("Failed save food into the database", e);
	    	throw new RuntimeException("Database error occurred while saving the food", e);
	    }
	}
	
	@Caching(evict = {
		@CacheEvict(value = "foods", allEntries = true),
		@CacheEvict(value = "food", key = "#id")
	})
	public ResponseEntity<FoodResponse> updateFood(Long id, FoodRequest updatedFood) {
		try {
			log.info("Service : Update food by id : {} , new details : {}",id,updatedFood);
			// 1. Fetch the existing food
			Food existingFood = foodRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
			
			// 2. Update the fields
			if(updatedFood.getFoodDescription() != null) {
				existingFood.setFoodDescription(updatedFood.getFoodDescription());
			}
			if(updatedFood.getFoodName() != null) {
				existingFood.setFoodName(updatedFood.getFoodName());
			}
			if (updatedFood.getFoodPrice() != null) {
			    existingFood.setFoodPrice(updatedFood.getFoodPrice());
			}
			
			// 3. Save
			foodRepository.save(existingFood);
			FoodResponse response = new FoodResponse(
					existingFood.getFoodId(),
					existingFood.getFoodName(),
					existingFood.getFoodDescription(),
					existingFood.getFoodPrice()
			);
			
			FoodResponseProto proto = FoodResponseProto.newBuilder()
			        .setFoodId(response.getFoodId())
			        .setFoodName(response.getFoodName())
			        .setFoodDescription(response.getFoodDescription())
			        .setFoodPrice(response.getFoodPrice().toString())
			        .build();
			log.info("Broadcasting food update to RabbitMQ: {}", response);
			foodEventPublisher.broadcastFoodUpdate(proto.toByteArray());
	        
			log.info("Updated existing food into database: {}", response);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (ResourceNotFoundException e) {
            throw e;
        }catch (Exception e) {
			log.error("Failed save food into the database", e);
	    	throw new RuntimeException("Database error occurred while saving the food", e);
	    }
	}
	
	@Caching(evict = {
	        @CacheEvict(value = "foods", allEntries = true),
	        @CacheEvict(value = "food", key = "#id")
	    })
    public void deleteFood(Long id) {
        try {
        	log.info("Service : Delete food by is : {}",id);
            // Check if it exists before deleting
            if (!foodRepository.existsById(id)) {
                throw new ResourceNotFoundException("Food not found with ID: " + id);
            }
            
            // Delete the item
            foodRepository.deleteById(id);
            
            // Corrected log statement with the id variable passed in
            log.info("Successfully deleted food from the database with ID: {}", id);
            
        } catch (ResourceNotFoundException e) {
            // Let our specific 404 exception pass through to the GlobalExceptionHandler
            throw e;
        } catch (Exception e) {
            // Catch and log any other unexpected database crashes (500 Error)
            log.error("Failed to delete food with ID: {}", id, e);
            throw new RuntimeException("Database error occurred while deleting the food item", e);
        }
	}
	
	@Cacheable(value = "food", key = "#id")
	public FoodResponse getFood(Long id) {
		try {
			log.info("Service : Get food by id : {}", id);
			Food food = foodRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
			log.info("Successfully retrived food from the database with ID: {}", id);
			return new FoodResponse(
					food.getFoodId(),
					food.getFoodName(),
					food.getFoodDescription(),
					food.getFoodPrice()
			);
		}catch(ResourceNotFoundException e) {
			throw e;
		}
	}
	
	@Cacheable(value = "food_proto", key = "#id")
	public FoodResponseProto getFoodAsProto(Long id) {
		try {
			Food food = foodRepository.findById(id)
		            .orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
			return FoodResponseProto.newBuilder()
		            .setFoodId(food.getFoodId())
		            .setFoodName(food.getFoodName())
		            .setFoodDescription(food.getFoodDescription())
		            .setFoodPrice(food.getFoodPrice().toString())
		            .build();
		} catch (ResourceNotFoundException e) {
			throw e;
		}
	}
}