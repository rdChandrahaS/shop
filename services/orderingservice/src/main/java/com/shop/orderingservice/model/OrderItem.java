package com.shop.orderingservice.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/**
 * This Class maps to Postgre database. This stores a single food data
 * OrderItem
 */
public class OrderItem {
    
    private Long foodId;
    private String name;

    @Min(value=1, message = "Quantity must be at least 1")
    private int quantity; 
   
    private BigDecimal pricePerUnit; 
}