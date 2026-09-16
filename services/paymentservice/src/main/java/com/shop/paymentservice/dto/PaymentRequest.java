package com.shop.paymentservice.dto;

import java.math.BigDecimal;

import com.shop.paymentservice.model.enums.PaymentMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotBlank(message = "Order ID is required")
    private String id;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Payment Mode is required")
    private PaymentMode mode;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
}
