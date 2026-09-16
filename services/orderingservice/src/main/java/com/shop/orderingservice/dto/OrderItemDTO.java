package com.shop.orderingservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    @NotNull(message = "Food ID is required")
    private Long foodId;

    // Display-only fields. OrderingService ignores client supplied name/price
    // and always uses the locked inventory snapshot.
    private String name;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100")
    private int quantity;

    private java.math.BigDecimal pricePerUnit;
}
