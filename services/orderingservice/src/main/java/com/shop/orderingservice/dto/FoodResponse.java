package com.shop.orderingservice.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodResponse {
	private Long foodId;
	private String foodName;
	private String foodDescription;
	private BigDecimal foodPrice;
}
