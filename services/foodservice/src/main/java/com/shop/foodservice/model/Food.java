package com.shop.foodservice.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="foods")
public class Food {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="food_id")
	private Long foodId;
	
	@Column(name="food_name")
	private String foodName;
	
	@Column(name="food_description")
	private String foodDescription;
	
	@Column(name="food_price")
	private BigDecimal foodPrice;
}