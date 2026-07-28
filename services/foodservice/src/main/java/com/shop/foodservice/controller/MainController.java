package com.shop.foodservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.foodservice.model.Food;
import com.shop.foodservice.service.FoodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class MainController {
	
	private final FoodService foodService;
	
	@GetMapping
	public List<Food> getFoods() {
		return foodService.getFoods();
	}
	
	@GetMapping("/{id}")
	public Food getFood(@PathVariable("id") Integer id) {
		return foodService.getFood(id);
	}	
}
