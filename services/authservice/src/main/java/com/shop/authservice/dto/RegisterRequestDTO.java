package com.shop.authservice.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequestDTO {

	@NotBlank(message = "Username cannot be empty")
	private String username;

	@NotBlank(message = "Email cannot be empty")
	private String email;
	
	@NotBlank(message = "Name cannot be empty")
    private String name;
    
    @NotBlank(message = "Password cannot be empty")
	private String password;
	
	private Set<String> roles;
}
