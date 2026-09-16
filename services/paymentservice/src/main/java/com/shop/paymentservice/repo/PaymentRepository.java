package com.shop.paymentservice.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.model.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByCustomerId(String customerId);
}
