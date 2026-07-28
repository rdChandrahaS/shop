package com.shop.orderingservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_stock")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "available_amount")
    private int availableAmount;
    
    @Column(name = "food_name")
    private String foodName;
}