package com.shop.orderingservice.dto;

import java.math.BigDecimal;
import java.util.List;

import com.shop.orderingservice.model.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDTO {
	
	private String orderId;
	private OrderStatus status;	
	private String message;
	private BigDecimal totalAmount;    
    private CustomerDTO customer;
    private List<OrderItemDTO> items;
    private String mode;
}
