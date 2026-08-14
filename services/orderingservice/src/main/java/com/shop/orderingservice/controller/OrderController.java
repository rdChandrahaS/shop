package com.shop.orderingservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public Order placeOrder(@Valid @RequestBody OrderEventDTO orderRequest, @AuthenticationPrincipal String tokenUserId) {
        // Pass the DTO and the token ID down to the service layer
        return orderService.processAndPlaceOrder(orderRequest, tokenUserId);
    }

    @GetMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public OrderStatus getOrderStatus(@PathVariable("orderId") String orderId, @AuthenticationPrincipal String tokenUserId) {
        
        // 1. Fetch the order from the database
        Order order = orderService.findById(orderId);
              
        // 2. Check if the current user has the ADMIN role
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities()
                                        .stream()
                                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // 3. Implement Resource Ownership Logic
        if (!isAdmin && !order.getCustomer().getCustomerId().equals(tokenUserId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "You do not have permission to view another customer's order."
            );
        }
        
        return order.getOrderStatus();
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/status")
    public Order updateOrderStatus(@PathVariable("orderId") String orderId, @RequestParam("newStatus") OrderStatus newStatus) {
        return orderService.updateStatus(orderId, newStatus);
    }
    
    @GetMapping("/test-auth")
    public Object testAuth(Authentication authentication) {
        return authentication.getAuthorities();
    }
}