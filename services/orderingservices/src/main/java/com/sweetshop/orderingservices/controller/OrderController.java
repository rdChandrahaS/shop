package com.sweetshop.orderingservices.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sweetshop.orderingservices.model.Order;
import com.sweetshop.orderingservices.model.enums.OrderStatus;
import com.sweetshop.orderingservices.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    
    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }
    
    @PostMapping("/newOrder")
    public Order placeOrder(@Valid @RequestBody Order newOrder) {
        return orderService.processAndPlaceOrder(newOrder);
    }

    @GetMapping("/{orderId}/status")
    public OrderStatus getOrderStatus(@PathVariable Long orderId) {
        Order order = orderService.findById(orderId);
        return order.getOrderStatus();
    }

    @PatchMapping("/{orderId}/status")
    public Order updateOrderStatus(@PathVariable Long orderId, @RequestParam OrderStatus newStatus) {
        return orderService.updateStatus(orderId, newStatus);
    }

    @GetMapping("/customer/{customerId}")
    public Page<Order> getCustomerOrders(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size){

        return orderService.getCustomerOrders(customerId, page, size);
    }
}
