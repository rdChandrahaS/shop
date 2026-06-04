package com.sweetshop.orderingservices.controller;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sweetshop.orderingservices.model.Food;
import com.sweetshop.orderingservices.service.FoodService;

@RestController
@RequestMapping("/foods")
public class FoodController {
    private final FoodService foodService;
    
    public FoodController(FoodService foodService){
        this.foodService = foodService;
    }

    @GetMapping("/availableFoods")
    public List<Food> availableFoods() {
        return foodService.fetchAvailableMenu();
    }
}
