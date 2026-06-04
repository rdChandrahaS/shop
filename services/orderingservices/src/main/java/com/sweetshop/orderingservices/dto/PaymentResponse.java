package com.sweetshop.orderingservices.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponse {
    
    private boolean success;
    private String transactionId;
    private String errorMessage;
}
