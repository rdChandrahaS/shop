package com.shop.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username is too long")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password is too long")
    private String password;
}
