package com.shop.orderingservice.consumer;

import java.math.BigDecimal;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.protobuf.FoodResponseProto;
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
            FoodResponseProto proto = FoodResponseProto.parseFrom(data);
            log.info("Received Async Food Update: {} - ${}", proto.getFoodName(), proto.getFoodPrice());

            Inventory inventory = inventoryRepository.findById(proto.getFoodId())
                    .orElse(new Inventory());
            
            inventory.setFoodId(proto.getFoodId());
            inventory.setFoodName(proto.getFoodName());
            inventory.setFoodPrice(new BigDecimal(proto.getFoodPrice()));
            
            // If it's a brand new menu item, give it some default stock
            if (inventory.getAvailableAmount() == 0) {
                inventory.setAvailableAmount(100); 
            }

            inventoryRepository.save(inventory);
        } catch (Exception e) {
            log.error("Failed to parse Food Update", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Food Update payload", e);
        }
    }
}