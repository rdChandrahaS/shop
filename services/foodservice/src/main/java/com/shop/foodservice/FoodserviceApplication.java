package com.shop.foodservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableMethodSecurity
public class FoodserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodserviceApplication.class, args);
	}

}
