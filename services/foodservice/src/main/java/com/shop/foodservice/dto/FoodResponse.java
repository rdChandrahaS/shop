package com.shop.foodservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FoodResponse {
	private Long foodId;
	private String foodName;
	private String foodDescription;
	private BigDecimal foodPrice;
}
