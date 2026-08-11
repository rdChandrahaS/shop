package com.shop.orderingservice.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.dto.PaymentResponse;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.protobuf.OrderEventProto;
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
	
	@RabbitListener(queues = {"${rabbitmq.queue.name}"} )
	public void consume(byte[] orderBytes) {
		try {
			
			OrderEventProto orderEvent = OrderEventProto.parseFrom(orderBytes);
			
			log.info("Successfully decoded Protobuf Event!");
            log.info("Order ID: {}", orderEvent.getOrderId());
            log.info("Customer Name: {}", orderEvent.getCustomer().getName());
            log.info("Total Amount: {}", orderEvent.getTotalAmount());
            
            
		}catch (Exception e) {
			log.error("Failed to parse Protobuf message", e);
		}
	}
	
	@RabbitListener(queues = {"${}"})
	public void handlePaymentResult(PaymentResponse response) {
		log.info("Received Payment Result for Order ID {}: Success={}", response.getOrderId(), response.isSuccess());

        Order order = orderRepository.findById(response.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + response.getOrderId()));

        if (response.isSuccess()) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} status updated to CONFIRMED", order.getOrderId());
        } else {
            orderService.revertInventory(order);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.warn("Order {} payment failed. Inventory reverted and order CANCELLED", order.getOrderId());
        }
	}
}
