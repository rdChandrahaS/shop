package com.shop.foodservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FoodRequest {

    @NotBlank(message = "Food name is required")
    @Size(min = 2, max = 120, message = "Food name must be between 2 and 120 characters")
    private String foodName;

    @NotBlank(message = "Food description is required")
    @Size(max = 500, message = "Food description cannot exceed 500 characters")
    private String foodDescription;

    @DecimalMin(value = "0.01", message = "Food price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Food price can have at most 2 decimal places")
    private BigDecimal foodPrice;

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Pattern(regexp = "https?://.+", message = "Image URL must start with http:// or https://")
    private String imageUrl;

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 60, message = "Category must be between 2 and 60 characters")
    private String category;

    private Boolean active = true;
}
