package com.sweetshop.orderingservices.model;


import java.time.LocalDateTime;
import java.util.List;

import com.sweetshop.orderingservices.model.enums.Delivery;
import com.sweetshop.orderingservices.model.enums.OrderStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long orderID;

    @OneToMany
    @NotEmpty(message = "An order must contain at least one item")
    @Valid()
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items;

    @Enumerated(EnumType.STRING)
    private Delivery deliveryDetails;
    
    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude 
    private Customer customer;

    private double totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
}
