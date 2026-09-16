package com.shop.paymentservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.Utils;
import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.exception.DuplicatePaymentException;
import com.shop.paymentservice.exception.PaymentNotFoundException;
import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.model.enums.PaymentMode;
import com.shop.paymentservice.model.enums.PaymentStatus;
import com.shop.paymentservice.repo.PaymentRepository;
import com.shop.paymentservice.strategy.PaymentStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Map<String, PaymentStrategy> paymentStrategies;
    private final PaymentRepository paymentRepo;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Boolean> paymentLockScript;
    private final PaymentEventPublisher paymentEventPublisher;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public PaymentResponse processPayment(PaymentRequest request) {
        validateRequest(request);

        Payment existing = paymentRepo.findById(request.getId()).orElse(null);
        if (existing != null) {
            verifyCustomer(existing, request.getCustomerId());
            verifyAmount(existing, request.getAmount());

            if (existing.getStatus() == PaymentStatus.SUCCESS
                    || existing.getStatus() == PaymentStatus.PENDING) {
                return new PaymentResponse(existing.getStatus(),
                        existing.getTransactionId() != null ? existing.getTransactionId() : existing.getGatewayOrderId(),
                        existing.getAmount());
            }

            if (existing.getStatus() == PaymentStatus.REFUND_REQUESTED
                    || existing.getStatus() == PaymentStatus.REFUNDED) {
                throw new IllegalStateException("This payment cannot be processed again.");
            }
        }

        String lockKey = "payment:lock:" + request.getId();
        Boolean acquired = redisTemplate.execute(paymentLockScript, List.of(lockKey), "300");
        if (!Boolean.TRUE.equals(acquired)) {
            throw new DuplicatePaymentException("A payment is already being processed for this order.");
        }

        try {
            PaymentStrategy strategy = paymentStrategies.get(request.getMode().name());
            if (strategy == null) {
                throw new IllegalArgumentException("Unsupported payment mode: " + request.getMode());
            }

            Payment payment = existing != null ? existing : new Payment();
            payment.setOrderId(request.getId());
            payment.setCustomerId(request.getCustomerId());
            payment.setAmount(request.getAmount());
            payment.setMode(request.getMode());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setGatewayOrderId(null);
            payment.setTransactionId(null);

            try {
                paymentRepo.saveAndFlush(payment);
            } catch (DataIntegrityViolationException e) {
                Payment concurrent = paymentRepo.findById(request.getId()).orElseThrow(() -> e);
                verifyCustomer(concurrent, request.getCustomerId());
                verifyAmount(concurrent, request.getAmount());
                return new PaymentResponse(concurrent.getStatus(),
                        concurrent.getTransactionId() != null ? concurrent.getTransactionId() : concurrent.getGatewayOrderId(),
                        concurrent.getAmount());
            }

            PaymentResponse response = strategy.processPayment(request);
            payment.setStatus(response.getStatus());

            if (request.getMode() == PaymentMode.ONLINE && response.getStatus() == PaymentStatus.PENDING) {
                payment.setGatewayOrderId(response.getTransactionId());
                payment.setTransactionId(null);
            } else {
                payment.setTransactionId(response.getTransactionId());
            }

            paymentRepo.saveAndFlush(payment);

            if (response.getStatus() == PaymentStatus.SUCCESS
                    || response.getStatus() == PaymentStatus.FAILED) {
                redisTemplate.delete(lockKey);
            }

            if (response.getStatus() == PaymentStatus.SUCCESS) {
                paymentEventPublisher.publishResult(payment, true, "Payment successful");
            } else if (response.getStatus() == PaymentStatus.FAILED) {
                paymentEventPublisher.publishResult(payment, false, "Payment failed");
            }

            return new PaymentResponse(payment.getStatus(),
                    payment.getTransactionId() != null ? payment.getTransactionId() : payment.getGatewayOrderId(),
                    payment.getAmount());
        } catch (RuntimeException e) {
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Transactional 
    public void handleRazorpayWebhook(String payload, String signature) {
        if (payload == null || payload.isBlank() || signature == null || signature.isBlank()) {
            throw new SecurityException("Missing Razorpay webhook signature or payload");
        }

        try {
            if (!Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                throw new SecurityException("Invalid Razorpay webhook signature");
            }

            JSONObject root = new JSONObject(payload);
            String event = root.optString("event");
            if (!event.equals("payment.captured") && !event.equals("payment.failed")) {
                log.info("Ignoring unsupported Razorpay event: {}", event);
                return;
            }

            JSONObject entity = root.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String gatewayOrderId = entity.optString("order_id", null);
            String gatewayPaymentId = entity.optString("id", null);
            if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
                throw new IllegalArgumentException("Razorpay webhook does not contain an order_id");
            }

            Payment payment = paymentRepo.findByGatewayOrderId(gatewayOrderId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for gateway order: " + gatewayOrderId));

            if (entity.has("amount")) {
                long gatewayAmountPaise = entity.getLong("amount");
                long expectedAmountPaise = payment.getAmount().movePointRight(2).longValueExact();
                if (gatewayAmountPaise != expectedAmountPaise) {
                    throw new IllegalArgumentException("Webhook amount does not match the payment amount");
                }
            }

            if (event.equals("payment.captured")) {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setTransactionId(gatewayPaymentId);
                    paymentRepo.save(payment);
                    paymentEventPublisher.publishResult(payment, true, "Payment captured by Razorpay");
                }
            } else {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setTransactionId(gatewayPaymentId);
                    paymentRepo.save(payment);
                    paymentEventPublisher.publishResult(payment, false, "Payment failed at Razorpay");
                }
            }

            redisTemplate.delete("payment:lock:" + payment.getOrderId());
        } catch (SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            throw new IllegalStateException("Unable to process Razorpay webhook", e);
        }
    }

    @Transactional
    public ResponseEntity<String> requestRefund(String orderId) {
        Payment payment = paymentRepo.findById(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Only successful payments can be refunded.");
        }

        if (payment.getMode() != PaymentMode.ONLINE || payment.getTransactionId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Refunds are currently supported only for successful online payments.");
        }

        payment.setStatus(PaymentStatus.REFUND_REQUESTED);
        paymentRepo.save(payment);
        return ResponseEntity.ok("Refund request submitted for manual review.");
    }

    public List<Payment> getPendingRefunds() {
        return paymentRepo.findByStatus(PaymentStatus.REFUND_REQUESTED);
    }

    @Transactional
    public ResponseEntity<String> approveRefund(String orderId) {
        Payment payment = paymentRepo.findById(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Payment is not pending a refund.");
        }

        PaymentStrategy strategy = paymentStrategies.get(payment.getMode().name());
        if (strategy == null || payment.getTransactionId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Refund is not supported for this payment.");
        }

        boolean refunded = strategy.processRefund(payment.getTransactionId(), payment.getAmount());
        if (!refunded) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Payment gateway refund failed. No payment status change was made.");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepo.save(payment);
        return ResponseEntity.ok("Refund approved and processed successfully.");
    }

    private void validateRequest(PaymentRequest request) {
        if (request == null || request.getId() == null || request.getId().isBlank()
                || request.getCustomerId() == null || request.getCustomerId().isBlank()
                || request.getAmount() == null || request.getAmount().signum() <= 0
                || request.getMode() == null) {
            throw new IllegalArgumentException("Invalid payment request");
        }
        if (request.getAmount().scale() > 2) {
            throw new IllegalArgumentException("Payment amount may contain at most two decimal places");
        }
    }

    private void verifyCustomer(Payment payment, String customerId) {
        if (!payment.getCustomerId().equals(customerId)) {
            throw new SecurityException("Payment does not belong to this customer");
        }
    }

    private void verifyAmount(Payment payment, BigDecimal requestedAmount) {
        if (payment.getAmount().compareTo(requestedAmount) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the existing order payment");
        }
    }
}
