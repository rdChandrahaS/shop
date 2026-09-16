package com.shop.orderingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
    @Column(name = "food_id", nullable = false)
    private Long foodId;

    @Column(name = "available_amount", nullable = false)
    private int availableAmount;

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Column(name = "food_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal foodPrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "category", length = 60)
    private String category;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
