package com.shop.orderingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.shop.orderingservice.dto.FoodResponse;

@FeignClient(name="foodservice")
public interface FoodClient {
    @GetMapping("/foods/{id}")
    FoodResponse getFood(@PathVariable("id") Long id);
}
