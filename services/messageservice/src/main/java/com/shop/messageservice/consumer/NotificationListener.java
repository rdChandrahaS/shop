package com.shop.messageservice.consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.shop.messageservice.protobuf.OrderEventProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationListener {

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void handleOrderEvent(byte[] payload) {
        final OrderEventProto event;

        try {
            event = OrderEventProto.parseFrom(payload);
        } catch (InvalidProtocolBufferException ex) {
            log.error("Invalid OrderEventProto received; sending message to DLQ", ex);
            throw new AmqpRejectAndDontRequeueException("Invalid protobuf payload", ex);
        }

        validate(event);

        try {
            log.info("Received order notification: orderId={}, status={}, customerEmail={}",
                    event.getOrderId(), event.getStatus(),
                    event.hasCustomer() ? event.getCustomer().getEmail() : "<missing>");

            // Integrate the actual email/SMS provider here when one is configured.
            // Keeping notification delivery outside the RabbitMQ transaction means a
            // provider failure throws and lets the configured listener retry policy run.
            deliverNotification(event);
        } catch (AmqpRejectAndDontRequeueException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Notification processing failed; RabbitMQ listener will retry", ex);
            throw new RuntimeException("Notification processing failed", ex);
        }
    }

    private void validate(OrderEventProto event) {
        if (event.getOrderId().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Order event has no order ID");
        }
        if (event.getStatus().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Order event has no status");
        }
        if (!event.hasCustomer() || event.getCustomer().getEmail().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Order event has no customer email");
        }
    }

    private void deliverNotification(OrderEventProto event) {
        log.info("Notification prepared for order {}: status={}, message={}",
                event.getOrderId(), event.getStatus(), event.getMessage());
    }
}
