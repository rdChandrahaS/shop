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
            Inventory inventory = inventoryRepository.findById(proto.getFoodId()).orElseGet(Inventory::new);
            boolean isNew = inventory.getFoodId() == null;

            inventory.setFoodId(proto.getFoodId());
            inventory.setFoodName(proto.getFoodName());
            inventory.setFoodPrice(new BigDecimal(proto.getFoodPrice()));
            inventory.setImageUrl(proto.getImageUrl());
            inventory.setCategory(proto.getCategory());
            inventory.setActive(proto.getActive());

            if (isNew) {
                inventory.setAvailableAmount(100);
            }

            inventoryRepository.save(inventory);
        } catch (Exception e) {
            log.error("Failed to process Food Update", e);
            throw new AmqpRejectAndDontRequeueException("Invalid Food Update payload", e);
        }
    }
}
