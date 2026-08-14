package com.shop.foodservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.foodservice.dto.FoodRequest;
import com.shop.foodservice.dto.FoodResponse;
import com.shop.foodservice.service.FoodService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class MainController {
	
	private final FoodService foodService;
	
	@GetMapping
	public List<FoodResponse> getFoods() {
		return foodService.getFoods();
	}
	
	@GetMapping("/{id}")
	public FoodResponse getFood(@PathVariable("id") Long id) {
		return foodService.getFood(id);
	}
	
	@PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FoodResponse> addFood(@Valid @RequestBody FoodRequest food) {
        return foodService.addFood(food);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FoodResponse> updateFood(@PathVariable("id") Long id, @Valid @RequestBody FoodRequest food) {
        return foodService.updateFood(id, food);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FoodResponse> deleteFood(@PathVariable("id") Long id) {
        foodService.deleteFood(id);
        return ResponseEntity.noContent().build();
    }
}
