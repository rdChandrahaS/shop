package com.shop.foodservice.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodEventPublisher {
	
	private final RabbitTemplate rabbitTemplate;
	
	private String exchange;
    private String routingKey;
    
    public void broadcastFoodUpdate(byte[] protobufData) {
    	try {
	    	rabbitTemplate.convertAndSend(exchange, routingKey, protobufData);
	        log.info("Successfully broadcasted Food update via RabbitMQ");
    	}catch (Exception e) {
            log.error("Failed to send Food update to RabbitMQ", e);
        }
    }
}
