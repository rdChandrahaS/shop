package com.sweetshop.orderingservices.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sweetshop.orderingservices.model.Food;
import com.sweetshop.orderingservices.model.Order;
import com.sweetshop.orderingservices.service.FoodService;
import com.sweetshop.orderingservices.service.OrderService;

@RestController
@RequestMapping("/orderFood")
public class MainController {

    private FoodController foodController;
    private OrderController orderController;

    public MainController(FoodService foodService,OrderService orderService) {
        foodController = new FoodController(foodService);
        orderController = new OrderController(orderService);
    }

    @GetMapping("/availableFoods")
    public List<Food> availableFoods() {
        return foodController.availableFoods();
    }

    @PostMapping("/placeOrder")
    public Order placeOrder(@RequestBody Order newOrder) {
        return orderController.placeOrder(newOrder);
    }
}
