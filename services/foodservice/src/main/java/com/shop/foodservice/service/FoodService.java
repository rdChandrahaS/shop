package com.shop.foodservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private FoodResponse toResponse(Food food) {
        return new FoodResponse(
            food.getFoodId(),
            food.getFoodName(),
            food.getFoodDescription(),
            food.getFoodPrice(),
            food.getImageUrl() == null || food.getImageUrl().isBlank() ? null : food.getImageUrl(),
            food.getCategory() == null || food.getCategory().isBlank() ? "Uncategorized" : food.getCategory(),
            food.isActive()
        );
    }

    private FoodResponseProto toProto(Food food) {
        return FoodResponseProto.newBuilder()
            .setFoodId(food.getFoodId())
            .setFoodName(food.getFoodName())
            .setFoodDescription(food.getFoodDescription())
            .setFoodPrice(food.getFoodPrice().toString())
            .setImageUrl(food.getImageUrl() == null ? "" : food.getImageUrl())
            .setCategory(food.getCategory() == null ? "Uncategorized" : food.getCategory())
            .setActive(food.isActive())
            .build();
    }

    @Cacheable(value = "foods")
    @Transactional(readOnly = true)
    public List<FoodResponse> getFoods() {
        return foodRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = "foods", allEntries = true)
    @Transactional
    public ResponseEntity<FoodResponse> addFood(FoodRequest request) {
        Food food = new Food();
        apply(food, request);
        Food saved = foodRepository.save(food);
        FoodResponse response = toResponse(saved);
        foodEventPublisher.broadcastFoodUpdate(toProto(saved).toByteArray());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Caching(evict = {
        @CacheEvict(value = "foods", allEntries = true),
        @CacheEvict(value = "food", key = "#id"),
        @CacheEvict(value = "food_proto", key = "#id")
    })
    @Transactional
    public ResponseEntity<FoodResponse> updateFood(Long id, FoodRequest request) {
        Food food = foodRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));

        apply(food, request);
        Food saved = foodRepository.save(food);
        foodEventPublisher.broadcastFoodUpdate(toProto(saved).toByteArray());

        return ResponseEntity.ok(toResponse(saved));
    }

    private void apply(Food food, FoodRequest request) {
        food.setFoodName(request.getFoodName().trim());
        food.setFoodDescription(request.getFoodDescription().trim());
        food.setFoodPrice(request.getFoodPrice());
        food.setImageUrl(request.getImageUrl().trim());
        food.setCategory(request.getCategory().trim());
        food.setActive(request.getActive() == null || request.getActive());
    }

    @Caching(evict = {
        @CacheEvict(value = "foods", allEntries = true),
        @CacheEvict(value = "food", key = "#id"),
        @CacheEvict(value = "food_proto", key = "#id")
    })
    @Transactional
    public void deleteFood(Long id) {
        if (!foodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Food not found with ID: " + id);
        }
        foodRepository.deleteById(id);
    }

    @Cacheable(value = "food", key = "#id")
    @Transactional(readOnly = true)
    public FoodResponse getFood(Long id) {
        return foodRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
    }

    @Cacheable(value = "food_proto", key = "#id")
    @Transactional(readOnly = true)
    public FoodResponseProto getFoodAsProto(Long id) {
        return foodRepository.findById(id)
            .map(this::toProto)
            .orElseThrow(() -> new ResourceNotFoundException("Food not found with ID: " + id));
    }
}
