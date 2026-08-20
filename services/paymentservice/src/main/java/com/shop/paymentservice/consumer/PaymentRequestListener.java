package com.shop.paymentservice.consumer;

import java.math.BigDecimal;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.shop.paymentservice.dto.PaymentRequest;
import com.shop.paymentservice.dto.PaymentResponse;
import com.shop.paymentservice.exception.DuplicatePaymentException;
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

            PaymentResponse paymentResult = paymentService.processPayment(request);
            
            if (paymentResult.getStatus() == PaymentStatus.PENDING && request.getMode() == PaymentMode.ONLINE) {
                log.info("Online Payment is PENDING for Order {}. Waiting for Gateway Webhook...", request.getId());
                return; 
            }
            
            boolean isSuccess = (paymentResult.getStatus() == PaymentStatus.SUCCESS || paymentResult.getStatus() == PaymentStatus.PENDING);
            String message = "Payment processed with status: " + paymentResult.getStatus();
            
            PaymentResponseProto responseProto = PaymentResponseProto.newBuilder()
                    .setOrderId(requestProto.getOrderId())
                    .setTransactionId(paymentResult.getTransactionId() != null ? paymentResult.getTransactionId() : "N/A")
                    .setSuccess(isSuccess)
                    .setMessage(message != null ? message : "Processed")
                    .build();

            rabbitTemplate.convertAndSend(exchangeName, resultRoutingKey, responseProto.toByteArray());
            log.info("Sent payment result back for Order ID: {}", requestProto.getOrderId());

        } catch (DuplicatePaymentException e) {
        	log.warn("Duplicate payment attempt detected. Discarding message.", e);
        } catch (InvalidProtocolBufferException e) {
            log.error("Fatal error: Failed to parse PaymentRequestProto. Sending straight to DLQ.", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Protobuf payload", e);
        } catch (Exception e) {
            log.error("Transient error processing payment. Spring will retry...", e);
            throw new RuntimeException("Transient payment error", e);
        }
    }
    
}
