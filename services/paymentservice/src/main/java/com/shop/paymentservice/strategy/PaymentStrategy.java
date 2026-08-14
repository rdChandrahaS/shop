package com.shop.paymentservice.strategy;

import java.math.BigDecimal;

import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;

public interface PaymentStrategy {
	PaymentResponse processPayment(PaymentRequest request);
	default boolean processRefund(String transactionId, BigDecimal refundAmount) {
		return false;
	}
}
