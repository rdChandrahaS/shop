package com.shop.orderingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Order placeOrder(@Valid @RequestBody OrderEventDTO orderRequest, Authentication authentication) {
        return orderService.processAndPlaceOrder(orderRequest, authentication);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<Order> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return orderService.getOrdersForCurrentUser(authentication, page, size);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Order getOrder(@PathVariable String orderId, Authentication authentication) {
        return orderService.getOrderForUser(orderId, authentication);
    }

    @GetMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public OrderStatus getOrderStatus(@PathVariable String orderId, Authentication authentication) {
        return orderService.getOrderForUser(orderId, authentication).getOrderStatus();
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Order updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus newStatus) {
        return orderService.updateStatus(orderId, newStatus);
    }
}
