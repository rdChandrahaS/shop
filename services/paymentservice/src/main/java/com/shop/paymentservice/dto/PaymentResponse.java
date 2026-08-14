package com.shop.paymentservice.dto;

import java.math.BigDecimal;

import com.shop.paymentservice.model.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {
	private PaymentStatus status;
    private String transactionId;
    private BigDecimal amountProcessed;
}
