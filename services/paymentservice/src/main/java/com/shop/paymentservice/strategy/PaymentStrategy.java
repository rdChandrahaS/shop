package com.shop.paymentservice.strategy;

import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;

public interface PaymentStrategy {
	PaymentResponse processPayment(PaymentRequest request);
	default boolean processRefund(String transactionId, double refundAmount) {
		return false;
	}
}
