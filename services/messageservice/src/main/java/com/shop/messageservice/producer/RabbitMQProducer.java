package com.shop.messageservice.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.shop.messageservice.dto.OrderEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMQProducer {
	
	@Value("${rabbitmq.exchange.name}")
	private String exchangeName;
	
	@Value("${rabbitmq.routing.key}")
	private String routingKey;
	
	private final RabbitTemplate rabbitTemplate;
	
	public ResponseEntity<String> sendMessage(String message) {
		log.info("Message sent : {}", message);
		rabbitTemplate.convertAndSend(exchangeName , routingKey , message);
		return ResponseEntity.ok("Message sent to ");
	}
	
	public ResponseEntity<String> sendOrderEvent(OrderEventDTO orderEvent){
		log.info("Sending JSON Event: {}", orderEvent);
		rabbitTemplate.convertAndSend(exchangeName, routingKey, orderEvent);
		return ResponseEntity.ok("Order Created");
	}
}
