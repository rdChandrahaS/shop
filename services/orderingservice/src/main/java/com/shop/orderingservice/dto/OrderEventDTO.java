package com.shop.orderingservice.dto;

import java.math.BigDecimal;
import java.util.List;

import com.shop.orderingservice.model.enums.OrderStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDTO> items;

    @NotBlank(message = "Payment mode is required")
    private String mode;
}
