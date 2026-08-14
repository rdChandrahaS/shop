package com.shop.orderingservice.model;

import org.springframework.data.annotation.Id;

import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.shop.orderingservice.model.enums.OrderStatus;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(collection="orders")
public class Order {

    @Id
    private String orderId;

    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private BigDecimal totalAmount;
    private Customer customer; 
    private List<OrderItem> orderDetails; 
}