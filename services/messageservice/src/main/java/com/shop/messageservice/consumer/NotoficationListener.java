package com.shop.messageservice.consumer;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.shop.messageservice.protobuf.OrderEventProto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotoficationListener {
	
	@RabbitListener(queues="${rabbitmq.queue.name}")
	public void handleOrderEvent(byte[] eventBytes) {
		try {
			OrderEventProto orderEvent = OrderEventProto.parseFrom(eventBytes);
            log.info("Received Protobuf Order Event for Order ID: {}", orderEvent.getOrderId());
            log.info("Total Amount: ${}", orderEvent.getTotalAmount());
            log.info("Customer Email: {}", orderEvent.getCustomer().getEmail());
            
         // TODO: Trigger Email/SMS logic here
            
		} catch (InvalidProtocolBufferException e) {
			log.error("Fatal error: Failed to parse OrderEventProto bytes. Sending to DLQ.", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Protobuf payload", e);
		}catch (Exception e) {
	        // TRANSIENT: Something else went wrong (like an Email API timeout).
	        // Throwing a standard exception triggers Spring's RabbitMQ retry mechanism.
	        log.error("Transient error while processing notification. Spring will retry...", e);
	        throw new RuntimeException("Failed to process notification", e);
	    }
	}
}
