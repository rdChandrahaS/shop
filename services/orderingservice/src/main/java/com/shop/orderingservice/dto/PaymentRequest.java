package com.shop.orderingservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
	private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String paymentMode;
}
