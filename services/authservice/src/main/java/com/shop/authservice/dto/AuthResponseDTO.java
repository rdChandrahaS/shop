package com.shop.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AuthResponseDTO {
	private String token;
	private String id;
	private String username;
}
