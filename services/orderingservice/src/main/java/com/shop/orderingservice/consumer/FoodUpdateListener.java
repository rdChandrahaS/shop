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

            java.util.Optional<Inventory> existingInventory = inventoryRepository.findById(proto.getFoodId());
            Inventory inventory = existingInventory.orElseGet(Inventory::new);
            boolean isNewInventory = existingInventory.isEmpty();

            inventory.setFoodId(proto.getFoodId());
            inventory.setFoodName(proto.getFoodName());
            inventory.setFoodPrice(new BigDecimal(proto.getFoodPrice()));
            
            // Only initialize stock for a genuinely new inventory record.
            // An existing item with zero stock must remain out of stock.
            if (isNewInventory) {
                inventory.setAvailableAmount(100);
            }

            inventoryRepository.save(inventory);
        } catch (Exception e) {
            log.error("Failed to parse Food Update", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Food Update payload", e);
        }
    }
}