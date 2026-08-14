package com.shop.orderingservice.consumer;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.exception.OrderNotFoundException;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.protobuf.PaymentResponseProto;
import com.shop.orderingservice.repo.OrderRepository;
import com.shop.orderingservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConsumer {
	
	private final OrderRepository orderRepository;
	private final OrderService orderService;
	
	@RabbitListener(queues = {"${payment.response.queue}"})
	public void handlePaymentResult(byte[] responseBytes) {
		
		try {
			PaymentResponseProto response = PaymentResponseProto.parseFrom(responseBytes);
			log.info("Received Payment Result for Order ID {}: Success={}", response.getOrderId(), response.getSuccess());

			Order order = orderRepository.findById(response.getOrderId())
					.orElseThrow(() -> new OrderNotFoundException("Order not found: " + response.getOrderId()));

			if (response.getSuccess()) {
				order.setOrderStatus(OrderStatus.CONFIRMED);
				orderRepository.save(order);
				log.info("Order {} status updated to CONFIRMED", order.getOrderId());
			} else {
				orderService.revertInventory(order);
				order.setOrderStatus(OrderStatus.CANCELLED);
				orderRepository.save(order);log.warn("Order {} payment failed. Inventory reverted and order CANCELLED", order.getOrderId());
			}
		} catch (Exception e) {
			log.error("Failed to parse PaymentResponseProto bytes", e);
			throw new AmqpRejectAndDontRequeueException("Failed to process payment result", e);
		}
	}
}
