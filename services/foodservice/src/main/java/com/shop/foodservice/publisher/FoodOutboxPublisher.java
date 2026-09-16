package com.shop.foodservice.publisher;

import java.time.Instant;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shop.foodservice.model.FoodOutboxEvent;
import com.shop.foodservice.repo.FoodOutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FoodOutboxPublisher {

    private static final String FOOD_EXCHANGE = "food.exchange";
    private static final String FOOD_ROUTING_KEY = "food.update";

    private final FoodOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${food.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<FoodOutboxEvent> events =
                outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();

        for (FoodOutboxEvent event : events) {
            rabbitTemplate.convertAndSend(
                    FOOD_EXCHANGE,
                    FOOD_ROUTING_KEY,
                    event.getPayload()
            );
            event.setPublishedAt(Instant.now());
        }

        if (!events.isEmpty()) {
            outboxRepository.saveAll(events);
        }
    }
}
