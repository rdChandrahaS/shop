package com.shop.paymentservice.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.shop.paymentservice.model.Payment;
import com.shop.paymentservice.protobuf.PaymentResponseProto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${payment.exchange.key}")
    private String exchangeName;

    @Value("${payment.result.routing.key}")
    private String resultRoutingKey;

    public void publishResult(Payment payment, boolean success, String message) {
        PaymentResponseProto response = PaymentResponseProto.newBuilder()
                .setOrderId(payment.getOrderId())
                .setTransactionId(payment.getTransactionId() != null ? payment.getTransactionId() : "N/A")
                .setSuccess(success)
                .setMessage(message)
                .build();

        rabbitTemplate.convertAndSend(exchangeName, resultRoutingKey, response.toByteArray());
    }
}
