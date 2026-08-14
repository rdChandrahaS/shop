package com.shop.paymentservice.strategy;

import org.springframework.stereotype.Component;
import java.util.UUID;

import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.model.enums.PaymentStatus;

@Component("UPI")
public class UPIPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
    	return new PaymentResponse(PaymentStatus.PENDING, "txn_upi_" + UUID.randomUUID().toString(), request.getAmount());
    }
}