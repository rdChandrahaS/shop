package com.shop.foodservice.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.foodservice.dto.FoodRequest;
import com.shop.foodservice.dto.FoodResponse;
import com.shop.foodservice.exception.ResourceNotFoundException;
import com.shop.foodservice.model.Food;
import com.shop.foodservice.model.FoodOutboxEvent;
import com.shop.foodservice.model.FoodOutboxEvent.EventType;
import com.shop.foodservice.proto.FoodEvent;
import com.shop.foodservice.repo.FoodOutboxRepository;
import com.shop.foodservice.repo.FoodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;
    private final FoodOutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "foods")
    public List<FoodResponse> getFoods() {
        return foodRepository.findAllByActiveTrueOrderByFoodNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FoodResponse getFood(Long id) {
        return toResponse(findFood(id));
    }

    @Transactional
    @CacheEvict(value = "foods", allEntries = true)
    public FoodResponse addFood(FoodRequest request) {
        Food food = new Food();
        apply(food, request);

        Food saved = foodRepository.save(food);
        saveOutbox(EventType.CREATED, saved);

        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "foods", allEntries = true)
    public FoodResponse updateFood(Long id, FoodRequest request) {
        Food food = findFood(id);
        apply(food, request);

        Food saved = foodRepository.save(food);
        saveOutbox(EventType.UPDATED, saved);

        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "foods", allEntries = true)
    public void deleteFood(Long id) {
        Food food = findFood(id);

        saveOutbox(EventType.DELETED, food);
        foodRepository.delete(food);
    }

    private Food findFood(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Food not found with id: " + id));
    }

    private void apply(Food food, FoodRequest request) {
        food.setFoodName(request.getFoodName().trim());
        food.setFoodDescription(request.getFoodDescription().trim());
        food.setFoodPrice(request.getFoodPrice());
        food.setImageUrl(request.getImageUrl().trim());
        food.setCategory(request.getCategory().trim());
        food.setActive(request.getActive());
    }

    private FoodResponse toResponse(Food food) {
        FoodResponse response = new FoodResponse();
        response.setFoodId(food.getFoodId());
        response.setFoodName(food.getFoodName());
        response.setFoodDescription(food.getFoodDescription());
        response.setFoodPrice(food.getFoodPrice());
        response.setImageUrl(food.getImageUrl());
        response.setCategory(food.getCategory());
        response.setActive(food.isActive());
        return response;
    }

    private void saveOutbox(EventType eventType, Food food) {
        FoodEvent event = FoodEvent.newBuilder()
                .setEventType(
                        switch (eventType) {
                            case CREATED -> FoodEvent.EventType.CREATED;
                            case UPDATED -> FoodEvent.EventType.UPDATED;
                            case DELETED -> FoodEvent.EventType.DELETED;
                        }
                )
                .setFood(toProto(food))
                .build();

        outboxRepository.save(
                new FoodOutboxEvent(eventType, food.getFoodId(), event.toByteArray())
        );
    }

    private com.shop.foodservice.proto.Food toProto(Food food) {
        return com.shop.foodservice.proto.Food.newBuilder()
                .setFoodId(food.getFoodId())
                .setFoodName(food.getFoodName())
                .setFoodDescription(food.getFoodDescription())
                .setFoodPrice(food.getFoodPrice().toPlainString())
                .setImageUrl(food.getImageUrl())
                .setCategory(food.getCategory())
                .setActive(food.isActive())
                .build();
    }
}
