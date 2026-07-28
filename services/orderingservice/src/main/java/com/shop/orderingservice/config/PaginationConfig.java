package com.shop.orderingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@ConfigurationProperties(prefix = "val.page")
@PropertySource("classpath:order.properties")
@Validated
public class PaginationConfig {
    @Min(0)
    private int safePage;
    
    @Min(0) 
    @Max(100)
    private int safeSize;
}
