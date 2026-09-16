package com.shop.orderingservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 180, message = "Email cannot exceed 180 characters")
    private String email;

    @Pattern(regexp = "^$|^[0-9+() -]{7,20}$", message = "Phone number is invalid")
    private String phoneNo;
}
