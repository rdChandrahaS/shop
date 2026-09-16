package com.shop.paymentservice.model;

import java.math.BigDecimal;

import com.shop.paymentservice.model.enums.PaymentMode;
import com.shop.paymentservice.model.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_details")
public class Payment {
    @Id
    @Column(name = "order_id", unique = true, nullable = false, updatable = false)
    private String orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Column(name = "order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_mode", nullable = false)
    private PaymentMode mode;

    @Column(name = "gateway_order_id", unique = true)
    private String gatewayOrderId;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
