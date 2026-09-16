package com.shop.paymentservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('USER')")
    public PaymentResponse processPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") String userId) {
        request.setCustomerId(userId);
        return paymentService.processPayment(request);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleRazorpayWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund/request/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> requestRefund(@PathVariable String orderId) {
        return paymentService.requestRefund(orderId);
    }

    @GetMapping("/refund/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Payment> getPendingRefunds() {
        return paymentService.getPendingRefunds();
    }

    @PostMapping("/refund/approve/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveRefund(@PathVariable String orderId) {
        return paymentService.approveRefund(orderId);
    }
}
