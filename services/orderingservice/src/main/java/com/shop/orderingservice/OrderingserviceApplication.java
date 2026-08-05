package com.shop.orderingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.shop.orderingservice.config.PaginationConfig;


@SpringBootApplication
@EnableConfigurationProperties({PaginationConfig.class})
@EnableFeignClients
@EnableMethodSecurity
public class OrderingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderingserviceApplication.class, args);
	}

}
