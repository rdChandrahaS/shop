package com.shop.orderingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
	private String orderId;
    private String customerId;
    private double amount;
    private String paymentMode;
}
