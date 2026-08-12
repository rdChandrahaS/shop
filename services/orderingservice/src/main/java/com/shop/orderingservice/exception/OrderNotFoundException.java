package com.shop.orderingservice.exception;

public class OrderNotFoundException extends RuntimeException{
	public OrderNotFoundException(String message) {
        super(message);
    }
}
