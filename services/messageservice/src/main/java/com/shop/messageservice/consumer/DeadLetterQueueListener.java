package com.shop.messageservice.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DeadLetterQueueListener {

    @RabbitListener(queues = "${rabbitmq.dlq.name}")
    public void processFailedMessage(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        List<Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();
        Object exception = message.getMessageProperties().getHeaders().get("x-exception-message");

        log.error("Message Service DLQ message received: routingKey={}, xDeath={}, exception={}",
                routingKey, xDeath, exception);

        if (log.isDebugEnabled() && message.getBody() != null) {
            log.debug("DLQ payload (UTF-8, if textual): {}",
                    new String(message.getBody(), StandardCharsets.UTF_8));
        }
    }
}
