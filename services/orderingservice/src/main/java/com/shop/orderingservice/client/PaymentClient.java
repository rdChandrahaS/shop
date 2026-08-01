package com.shop.orderingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shop.orderingservice.dto.PaymentResponse;

@FeignClient(name="PAYMENT-SERVICE" , url="${payment.client.url}")
public interface PaymentClient {
    @PostMapping("/api/payment/process")
    PaymentResponse processPayment(@RequestParam("customerId") String customerId, @RequestParam("amount") double amount);
}
