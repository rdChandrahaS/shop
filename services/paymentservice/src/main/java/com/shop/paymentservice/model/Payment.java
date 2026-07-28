package com.shop.paymentservice.model;

import com.shop.paymentservice.model.enums.PaymentMode;
import com.shop.paymentservice.model.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="payment_details")
public class Payment {
	
	@Id
	@Column(name="order_id", unique = true, nullable = false)
	private String orderId;
	
	@Column(name="order_amount")
	private double amount;
	
	@Enumerated(EnumType.STRING)
	@Column(name="order_status")
	private PaymentStatus status;
	
	@Enumerated(EnumType.STRING)
	@Column(name="order_mode")
	private PaymentMode mode;
	
	@Column(name="transaction_id")
	private String transactionId;
	
}
