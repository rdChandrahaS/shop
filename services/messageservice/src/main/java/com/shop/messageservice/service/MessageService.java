package com.shop.messageservice.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
	
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
}
