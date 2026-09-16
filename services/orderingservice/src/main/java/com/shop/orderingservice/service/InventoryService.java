package com.shop.orderingservice.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.orderingservice.dto.OrderItemDTO;
import com.shop.orderingservice.exception.InsufficientInventoryException;
import com.shop.orderingservice.exception.ResourceNotFoundException;
import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.OrderItem;
import com.shop.orderingservice.repo.InventoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * PostgreSQL-only transaction. Pessimistic locks prevent two concurrent
     * orders from reserving the same stock. IDs are locked in deterministic
     * order to reduce deadlock risk when orders contain multiple foods.
     */
    @Transactional("transactionManager")
    public void reserveAndPrice(Order order, List<OrderItemDTO> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (OrderItemDTO item : requestedItems) {
            if (item == null || item.getFoodId() == null || item.getFoodId() <= 0) {
                throw new IllegalArgumentException("Each order item must have a valid food ID");
            }
            if (item.getQuantity() <= 0 || item.getQuantity() > 100) {
                throw new IllegalArgumentException("Each item quantity must be between 1 and 100");
            }
        }

        Map<Long, Integer> quantities = requestedItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItemDTO::getFoodId,
                        LinkedHashMap::new,
                        Collectors.summingInt(OrderItemDTO::getQuantity)));

        List<Long> sortedIds = quantities.keySet().stream().sorted().toList();
        List<Inventory> stocks = new ArrayList<>();

        // Lock one row at a time in ascending food ID order.
        for (Long foodId : sortedIds) {
            Inventory stock = inventoryRepository.findByIdForUpdate(foodId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for food ID " + foodId));
            stocks.add(stock);
        }

        Map<Long, Inventory> byId = stocks.stream()
                .collect(Collectors.toMap(Inventory::getFoodId, value -> value));

        List<OrderItem> orderItems = new ArrayList<>(requestedItems.size());
        for (OrderItemDTO dto : requestedItems) {
            Inventory stock = byId.get(dto.getFoodId());

            if (!stock.isActive()) {
                throw new IllegalArgumentException(stock.getFoodName() + " is currently unavailable");
            }

            int required = quantities.get(dto.getFoodId());
            if (stock.getAvailableAmount() < required) {
                throw new InsufficientInventoryException(
                        "Insufficient stock for " + stock.getFoodName()
                                + ". Available: " + stock.getAvailableAmount()
                                + ", requested: " + required);
            }

            if (stock.getFoodPrice() == null || stock.getFoodPrice().signum() <= 0) {
                throw new IllegalStateException("Invalid price configured for food ID " + dto.getFoodId());
            }

            OrderItem item = new OrderItem();
            item.setFoodId(stock.getFoodId());
            item.setName(stock.getFoodName());
            item.setQuantity(dto.getQuantity());
            item.setPricePerUnit(stock.getFoodPrice());
            orderItems.add(item);
        }

        order.setOrderDetails(orderItems);
        order.setTotalAmount(orderItems.stream()
                .map(i -> i.getPricePerUnit().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        for (Inventory stock : stocks) {
            int required = quantities.get(stock.getFoodId());
            stock.setAvailableAmount(stock.getAvailableAmount() - required);
        }
        inventoryRepository.saveAll(stocks);
    }

    @Transactional("transactionManager")
    public void release(Order order) {
        Map<Long, Integer> quantities = quantities(order);

        for (Long foodId : quantities.keySet().stream().sorted().toList()) {
            Inventory stock = inventoryRepository.findByIdForUpdate(foodId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for food ID " + foodId));

            long restored = (long) stock.getAvailableAmount() + quantities.get(foodId);
            if (restored > Integer.MAX_VALUE) {
                throw new IllegalStateException("Inventory amount overflow for food ID " + foodId);
            }
            stock.setAvailableAmount((int) restored);
            inventoryRepository.save(stock);
        }
    }

    private Map<Long, Integer> quantities(Order order) {
        if (order == null || order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("Order details cannot be empty");
        }

        return order.getOrderDetails().stream()
                .filter(item -> item != null && item.getFoodId() != null && item.getQuantity() > 0)
                .collect(Collectors.groupingBy(
                        OrderItem::getFoodId,
                        Collectors.summingInt(OrderItem::getQuantity)));
    }
}
