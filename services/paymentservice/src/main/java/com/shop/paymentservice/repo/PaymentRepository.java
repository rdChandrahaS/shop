package com.shop.paymentservice.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.model.enums.PaymentStatus;

import java.util.List;


public interface PaymentRepository extends JpaRepository<Payment, String>{
	
	Optional<Payment> findByTransactionId(String transactionId);
	
	@Query("SELECT i FROM Payment WHERE")
	List<Payment> findByStatus(PaymentStatus status);
}