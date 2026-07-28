package com.shop.paymentservice.dto;

import com.shop.paymentservice.model.enums.PaymentMode;

import lombok.Data;

@Data
public class PaymentRequest {
	private String id;
	private PaymentMode mode;
	private double amount;
}
