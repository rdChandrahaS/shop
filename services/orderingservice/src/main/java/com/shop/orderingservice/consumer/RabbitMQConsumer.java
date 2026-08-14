package com.shop.orderingservice.consumer;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.protobuf.PaymentResponseProto;
import com.shop.orderingservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConsumer {
	
	private final OrderService orderService;
	
	@RabbitListener(queues = {"${payment.response.queue}"})
	public void handlePaymentResult(byte[] responseBytes) {
		
		try {
			PaymentResponseProto response = PaymentResponseProto.parseFrom(responseBytes);
			log.info("Received Payment Result for Order ID {}: Success={}", response.getOrderId(), response.getSuccess());

			orderService.handlePaymentResult(response.getOrderId(), response.getSuccess());

			if (response.getSuccess()) {
				log.info("Order {} status updated to CONFIRMED", response.getOrderId());
			} else {
				log.warn("Order {} payment failed. Inventory reverted and order CANCELLED", response.getOrderId());
			}
		} catch (Exception e) {
			log.error("Failed to parse PaymentResponseProto bytes", e);
			throw new AmqpRejectAndDontRequeueException("Failed to process payment result", e);
		}
	}
}
