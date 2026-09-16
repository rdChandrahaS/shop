package com.shop.foodservice.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id", nullable = false, updatable = false)
    private Long foodId;

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(name = "food_name", nullable = false, length = 120)
    private String foodName;

    @NotBlank
    @Size(max = 500)
    @Column(name = "food_description", nullable = false, length = 500)
    private String foodDescription;

    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "food_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal foodPrice;

    @NotBlank
    @Size(max = 500)
    @Pattern(
        regexp = "https?://.+",
        message = "imageUrl must be a valid HTTP/HTTPS URL"
    )
    @Column(name = "image_url", nullable = false, length = 500, columnDefinition = "varchar(500) default ''")
    private String imageUrl;

    @NotBlank
    @Size(min = 2, max = 60)
    @Column(name = "category", nullable = false, length = 60, columnDefinition = "varchar(60) default 'Uncategorized'")
    private String category;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;
}
