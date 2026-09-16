package com.shop.foodservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodRequest {

    @NotBlank(message = "Food name is required")
    @Size(min = 2, max = 120, message = "Food name must be between 2 and 120 characters")
    private String foodName;

    @NotBlank(message = "Food description is required")
    @Size(max = 500, message = "Food description cannot exceed 500 characters")
    private String foodDescription;

    @NotNull(message = "Food price is required")
    @DecimalMin(value = "0.01", message = "Food price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Food price can have at most 2 decimal places")
    private BigDecimal foodPrice;

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Pattern(
        regexp = "https?://[^\\s]+",
        message = "Image URL must be a valid HTTP/HTTPS URL"
    )
    private String imageUrl;

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 60, message = "Category must be between 2 and 60 characters")
    private String category;

    private Boolean active = true;
}
