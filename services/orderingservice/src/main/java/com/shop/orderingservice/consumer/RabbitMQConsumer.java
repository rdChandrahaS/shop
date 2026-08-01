package com.shop.orderingservice.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.protobuf.OrderEventProto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RabbitMQConsumer {
	
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
}
