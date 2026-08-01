package com.shop.messageservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.messageservice.dto.OrderEventDTO;
import com.shop.messageservice.producer.RabbitMQProducer;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {
	
	private final RabbitMQProducer producer;
	
	@GetMapping("/publish")
	public ResponseEntity<String> sendMessage(@RequestParam("message") String message){
		return producer.sendMessage(message);
	}
	
	@PostMapping("/publishOrder")
	public ResponseEntity<String> sendOrderEvent(OrderEventDTO orderEvent){
		return producer.sendOrderEvent(orderEvent);
	}
}
