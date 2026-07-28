package com.shop.orderingservice.client;

import org.springframework.stereotype.Service;

import com.shop.orderingservice.dto.PaymentResponse;

@Service
public class PaymentConfig implements PaymentClient{
	
	@Override
	public PaymentResponse processPayment(String customerId, double amount) {
	    PaymentResponse response = new PaymentResponse(); // Uses @NoArgsConstructor
	    response.setSuccess(true);
	    response.setTransactionID("DUMMY_TXN_123");
	    response.setMessage("Payment mocked successfully");
	    
	    return response;
	}
}
