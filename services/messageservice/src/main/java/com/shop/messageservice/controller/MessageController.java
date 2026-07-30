package com.shop.messageservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.messageservice.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {
	
	private final MessageService service;
	
	@GetMapping("/publish")
	public ResponseEntity<String> sendMessage(@RequestParam("message") String message){
		return service.sendMessage(message);
	}
}
