package com.shop.paymentservice.consumer;

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
        log.error("========== PAYMENT DLQ ALERT ==========");
        
        String originalRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.error("Original Routing Key: {}", originalRoutingKey);

        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();
        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
            log.error("Failure Reason (x-death): {}", xDeathHeader.get(0).get("reason"));
        }

        Object exceptionMessage = message.getMessageProperties().getHeaders().get("x-exception-message");
        if (exceptionMessage != null) {
            log.error("Exception Message: {}", exceptionMessage);
        }

        log.error("=======================================");
    }
}