package com.shop.paymentservice.strategy;

import org.springframework.stereotype.Component;

import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.model.enums.PaymentStatus;

@Component("COD")
public class CODPaymentStrategy implements PaymentStrategy{
	@Override
	public PaymentResponse processPayment(PaymentRequest request) {
		return new PaymentResponse(PaymentStatus.PENDING,"COD_PENDING",request.getAmount());
	}
}
