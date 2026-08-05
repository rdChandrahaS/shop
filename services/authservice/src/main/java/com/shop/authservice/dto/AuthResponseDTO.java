package com.shop.authservice.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuthResponseDTO {
	private String token;
	private String id;
	private String username;
}
