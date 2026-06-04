package com.sweetshop.orderingservices.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sweetshop.orderingservices.model.Food;
import com.sweetshop.orderingservices.repository.FoodRepository;

@Service
public class FoodService {
    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository){
        this.foodRepository = foodRepository;
    }

    public List<Food> fetchAvailableMenu() {
        return foodRepository.findByAvailableGreaterThan(0);
    }
}
