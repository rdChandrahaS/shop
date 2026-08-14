package com.shop.orderingservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
	
	@NotNull(message = "Food ID is required")
	private Long foodId;
	private String name;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity; 
    private BigDecimal pricePerUnit; 
}
