package com.shop.orderingservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Transactional("transactionManager")
    public void reserveAndPrice(Order order, List<OrderItemDTO> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Map<Long, Integer> quantities = requestedItems.stream()
            .collect(Collectors.groupingBy(OrderItemDTO::getFoodId, Collectors.summingInt(OrderItemDTO::getQuantity)));

        if (quantities.values().stream().anyMatch(q -> q == null || q <= 0)) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        List<Inventory> stocks = inventoryRepository.findAllByIdInForUpdate(quantities.keySet());
        if (stocks.size() != quantities.size()) {
            Set<Long> found = stocks.stream().map(Inventory::getFoodId).collect(Collectors.toSet());
            Long missing = quantities.keySet().stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Inventory not found for food ID " + missing);
        }

        Map<Long, Inventory> byId = stocks.stream().collect(Collectors.toMap(Inventory::getFoodId, x -> x));
        List<OrderItem> orderItems = requestedItems.stream().map(dto -> {
            Inventory stock = byId.get(dto.getFoodId());
            if (!stock.isActive()) {
                throw new IllegalArgumentException(stock.getFoodName() + " is currently unavailable");
            }
            int required = quantities.get(dto.getFoodId());
            if (stock.getAvailableAmount() < required) {
                throw new InsufficientInventoryException("Out of stock: " + stock.getFoodName());
            }

            OrderItem item = new OrderItem();
            item.setFoodId(stock.getFoodId());
            item.setName(stock.getFoodName());
            item.setQuantity(dto.getQuantity());
            item.setPricePerUnit(stock.getFoodPrice());
            return item;
        }).toList();

        order.setOrderDetails(orderItems);
        order.setTotalAmount(orderItems.stream()
            .map(i -> i.getPricePerUnit().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        for (Inventory stock : stocks) {
            stock.setAvailableAmount(stock.getAvailableAmount() - quantities.get(stock.getFoodId()));
        }
        inventoryRepository.saveAll(stocks);
    }

    @Transactional("transactionManager")
    public void release(Order order) {
        Map<Long, Integer> quantities = quantities(order);
        List<Inventory> stocks = inventoryRepository.findAllByIdInForUpdate(quantities.keySet());
        for (Inventory stock : stocks) {
            stock.setAvailableAmount(stock.getAvailableAmount() + quantities.get(stock.getFoodId()));
        }
        inventoryRepository.saveAll(stocks);
    }

    private Map<Long, Integer> quantities(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("Order details cannot be empty");
        }
        return order.getOrderDetails().stream()
            .collect(Collectors.groupingBy(OrderItem::getFoodId, Collectors.summingInt(OrderItem::getQuantity)));
    }
}
