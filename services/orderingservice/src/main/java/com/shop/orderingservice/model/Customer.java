package com.shop.orderingservice.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Customer {
    private String customerId;
    private String name;
    private String phoneNo;
}
