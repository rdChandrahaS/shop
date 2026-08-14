package com.shop.paymentservice.consumer;

import java.math.BigDecimal;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.model.enums.PaymentMode;
import com.shop.paymentservice.model.enums.PaymentStatus;
import com.shop.paymentservice.protobuf.PaymentRequestProto;
import com.shop.paymentservice.protobuf.PaymentResponseProto;
import com.shop.paymentservice.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestListener {
	
	private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${payment.exchange.key}")
    private String exchangeName;

    @Value("${payment.result.routing.key}")
    private String resultRoutingKey;
    
    @RabbitListener(queues = "${payment.request.queue}")
    public void processPaymentRequest(byte[] requestBytes) {
    	try {
			PaymentRequestProto requestProto = PaymentRequestProto.parseFrom(requestBytes);
			log.info("Processing async payment request for Order ID: {}", requestProto.getOrderId());

            PaymentRequest request = new PaymentRequest();
            request.setId(requestProto.getOrderId());
            request.setAmount(new BigDecimal(requestProto.getAmount()));
            request.setMode(PaymentMode.valueOf(requestProto.getPaymentMode()));

            boolean isSuccess = false;
            String transactionId = "txn_processing";
            String message;
            
            try {
                PaymentStatus status = paymentService.processPayment(request);
                isSuccess = (status == PaymentStatus.SUCCESS || status == PaymentStatus.PENDING);
                message = "Payment processed with status: " + status;
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Business exception (e.g., Lua script blocked duplicate lock). 
                // Do not retry, just return a failure response.
                log.warn("Payment rejected for Order ID: {}", requestProto.getOrderId(), e);
                message = e.getMessage();
            } catch (Exception e) {
                // Transient infrastructure exception (DB down). Throw to trigger RabbitMQ retries.
                log.error("Transient error processing payment. Spring will retry...", e);
                throw new RuntimeException("Transient payment error", e);
            }
            PaymentResponseProto responseProto = PaymentResponseProto.newBuilder()
                    .setOrderId(requestProto.getOrderId())
                    .setTransactionId(transactionId)
                    .setSuccess(isSuccess)
                    .setMessage(message != null ? message : "Processed")
                    .build();

            rabbitTemplate.convertAndSend(exchangeName, resultRoutingKey, responseProto.toByteArray());
            log.info("Sent payment result back for Order ID: {}", requestProto.getOrderId());

        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            log.error("Fatal error: Failed to parse PaymentRequestProto. Sending straight to DLQ.", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Protobuf payload", e);
        }
    }
    
}
