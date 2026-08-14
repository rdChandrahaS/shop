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
import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.model.enums.PaymentStatus;
import com.shop.paymentservice.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment")
public class PaymentController {
	private final PaymentService paymentService;
	
	@PostMapping("/process")
	public PaymentStatus processPayment(
			@RequestBody PaymentRequest request,
			@RequestHeader("X-User-Id") String userId) {
		log.info("Processing payment for user: {}", userId);
		return paymentService.processPayment(request);
	}
	
	@PostMapping("/webhook")
	public void handleRazorpayWebhook(
			@RequestBody String payload,
			@RequestHeader("X-Razorpay-Signature") String signature) {
		paymentService.handleRazorpayWebhook(payload, signature);
	}
	
	@PostMapping("/refund/request/{orderId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	public ResponseEntity<String> requestRefund(
			@PathVariable("orderId") String orderId,
			@RequestBody String Reason) {
		return paymentService.requestRefund(orderId, Reason);
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
