package com.shop.paymentservice.consumer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.shop.paymentservice.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestListener {
	
	private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;
    
    private String paymentRequestQueue;
    
    public void processPaymentRequest(PaymentRequestEvent event) {
    	
    }
    
}
