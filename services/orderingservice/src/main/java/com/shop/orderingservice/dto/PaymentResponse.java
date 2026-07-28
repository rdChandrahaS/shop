package com.shop.orderingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class PaymentResponse {
    private boolean success;
    private String transactionID;
    private String message;
}
