package com.shop.orderingservice.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DeadLetterQueueListener {

    @RabbitListener(queues = "${rabbitmq.dlq.name}")
    public void processFailedMessage(Message message) {
        log.error("========== DEAD LETTER QUEUE ALERT ==========");
        
        // 1. Extract the original routing key to know where it was supposed to go
        String originalRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.error("Original Routing Key: {}", originalRoutingKey);

        // 2. Extract RabbitMQ's x-death headers to see exactly why it was rejected/dead-lettered
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            log.error("Failure Reason (x-death): {}", xDeathHeader.get(0).get("reason"));
        }

        // 3. Extract the Spring exception message if available
        Object exceptionMessage = message.getMessageProperties().getHeaders().get("x-exception-message");
        if (exceptionMessage != null) {
            log.error("Exception Message: {}", exceptionMessage);
        }

        // 4. Extract the raw payload
        byte[] payload = message.getBody();
        log.error("Failed Payload Size: {} bytes", payload != null ? payload.length : 0);
        
        log.error("=============================================");

        // Future Enhancement: 
        // Save this raw payload and metadata to a `failed_events` PostgreSQL table
        // or trigger an alert to an admin dashboard so it can be manually reviewed and re-queued.
    }
}