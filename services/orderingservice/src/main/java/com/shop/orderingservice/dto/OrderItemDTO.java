package com.shop.orderingservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
	
	private Long FoodId;
	private String name;
    private int quantity; 
    private BigDecimal pricePerUnit; 
}
