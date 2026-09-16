package com.shop.orderingservice.consumer;

import java.math.BigDecimal;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.protobuf.Food;
import com.shop.orderingservice.protobuf.FoodEvent;
import com.shop.orderingservice.repo.InventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FoodUpdateListener {

    private final InventoryRepository inventoryRepository;

    @RabbitListener(queues = "ordering_food_update_queue")
    public void handleFoodUpdate(byte[] data) {
        try {
            FoodEvent event = FoodEvent.parseFrom(data);
            Food food = event.getFood();

            if (food.getFoodId() <= 0) {
                throw new IllegalArgumentException("Food event contains an invalid food ID");
            }

            Inventory inventory = inventoryRepository.findById(food.getFoodId()).orElseGet(Inventory::new);
            boolean isNew = inventory.getFoodId() == null;

            inventory.setFoodId(food.getFoodId());
            inventory.setFoodName(food.getFoodName());
            inventory.setFoodPrice(new BigDecimal(food.getFoodPrice()));
            inventory.setImageUrl(food.getImageUrl());
            inventory.setCategory(food.getCategory());
            inventory.setActive(event.getEventType() != FoodEvent.EventType.DELETED && food.getActive());

            if (isNew) {
                inventory.setAvailableAmount(100);
            }

            inventoryRepository.save(inventory);
        } catch (Exception e) {
            log.error("Failed to process Food update event", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Food update payload", e);
        }
    }
}
