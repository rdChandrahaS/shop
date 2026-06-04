package com.sweetshop.orderingservices.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sweetshop.orderingservices.dto.PaymentResponse;

@FeignClient(name="payment-service" , url="http:/localhost:8081")
public interface PaymentClient {

    @PostMapping("/api/payments/process")
    PaymentResponse processPayment(@RequestParam("customerId") Long customerId, @RequestParam("amount") double amount);
}
