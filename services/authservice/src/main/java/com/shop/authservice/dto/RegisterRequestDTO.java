package com.shop.authservice.dto;

import java.util.Set;

import lombok.Data;

@Data
public class RegisterRequestDTO {
	private String username;
	private String email;
	private String name;
	private String password;
	private Set<String> roles;
}
