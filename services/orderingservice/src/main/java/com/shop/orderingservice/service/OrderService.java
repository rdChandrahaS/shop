package com.shop.orderingservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.orderingservice.client.FoodClient;
import com.shop.orderingservice.config.PaginationConfig;
import com.shop.orderingservice.dto.FoodResponse;
import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.exception.InsufficientInventoryException;
import com.shop.orderingservice.exception.OrderNotFoundException;
import com.shop.orderingservice.model.Customer;
import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.OrderItem;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.protobuf.PaymentRequestProto;
import com.shop.orderingservice.repo.InventoryRepository;
import com.shop.orderingservice.repo.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaginationConfig paginationConfig;
    private final FoodClient foodClient;
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing.key}")
    private String requestRoutingKey;

    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new OrderNotFoundException("Order not found with ID: " + orderId)
                );
    }
    
    @Transactional
    public Order processAndPlaceOrder(OrderEventDTO orderRequest, String tokenUserId) {
        
    	// 1. MAP DTO TO ENTITY
        Order newOrder = new Order();
        
        Customer customer = new Customer();
        customer.setCustomerId(tokenUserId); // Set ID from JWT
        
        if (orderRequest.getCustomer() != null) {
            customer.setName(orderRequest.getCustomer().getName());
            customer.setEmail(orderRequest.getCustomer().getEmail());
            customer.setPhoneNo(orderRequest.getCustomer().getPhoneNo());
        }
        newOrder.setCustomer(customer);
        
		BigDecimal calculatedTotal = BigDecimal.ZERO;
        
        if (orderRequest.getItems() != null) {
            List<OrderItem> items = orderRequest.getItems()
            									.stream()
            									.map(dto -> {
                FoodResponse realFood = foodClient.getFood(dto.getFoodId());
                OrderItem item = new OrderItem();
                item.setName(realFood.getFoodName());
                item.setQuantity(dto.getQuantity());
                item.setPricePerUnit(realFood.getFoodPrice());
                item.setFoodId(dto.getFoodId());
                return item;
            }).collect(Collectors.toList());
            
            // Secure BigDecimal math: Price * Quantity, then sum it all up
            calculatedTotal = items.stream()
                    .map(item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            newOrder.setOrderDetails(items);
        }
        
        newOrder.setTotalAmount(calculatedTotal);
        
        // 2. DEDUCT INVENTORY (Fast Transaction)
        deductInventory(newOrder);

        // 3. Save order as pending
        newOrder.setOrderStatus(OrderStatus.PENDING);
        newOrder.setOrderDate(LocalDateTime.now());
        Order savedOrder;
        
        try {
            savedOrder = orderRepository.save(newOrder);
        } catch (Exception e) {
            revertInventory(newOrder); // Roll-back PostgresDB if MongoDB fails
            throw new RuntimeException("Failed to save order to database", e);
        }
        
        // 4. Asynchronously send Payment Request via Protobuf Binary
        PaymentRequestProto paymentRequestProto = PaymentRequestProto.newBuilder()
            .setOrderId(savedOrder.getOrderId())
            .setCustomerId(tokenUserId)
            // Convert BigDecimal to String to prevent precision loss over the wire
            .setAmount(savedOrder.getTotalAmount().toString()) 
            .setPaymentMode(orderRequest.getMode())
            .build();

        try {
            // Send raw bytes through RabbitMQ using the routing key
            rabbitTemplate.convertAndSend(exchangeName, requestRoutingKey, paymentRequestProto.toByteArray());
        } catch(Exception e) {
            revertInventory(newOrder); 
            orderRepository.delete(savedOrder); 
            throw new RuntimeException("Message broker unreachable. Order cancelled to prevent data corruption.", e);
        }

        return savedOrder;
    }

    public void deductInventory(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
        	throw new IllegalArgumentException("Order details cannot be empty!");
        }
        
        Map<Long, Integer> itemQuantities = order.getOrderDetails()
        		.stream()
                .collect(Collectors.groupingBy(OrderItem::getFoodId, Collectors.summingInt(OrderItem::getQuantity)));

        List<Inventory> stocks = inventoryRepository.findAllByIdInForUpdate(itemQuantities.keySet());

        if (stocks.size() != itemQuantities.size()) {
            throw new RuntimeException("Database mismatch: Missing inventory records.");
        }

        for (Inventory stock : stocks) {
            int required = itemQuantities.get(stock.getFoodId());
            if (required <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            if (stock.getAvailableAmount() < required) {
                throw new InsufficientInventoryException("Out of stock: " + stock.getFoodName());
            }
            stock.setAvailableAmount(stock.getAvailableAmount() - required);
        }
        inventoryRepository.saveAll(stocks);
    }

    public void revertInventory(Order order) {
        Map<Long, Integer> itemQuantities = order.getOrderDetails().stream()
                .collect(Collectors.groupingBy(OrderItem::getFoodId, Collectors.summingInt(OrderItem::getQuantity)));

        // Re-acquire locks to add stock back safely
        List<Inventory> stocks = inventoryRepository.findAllByIdInForUpdate(itemQuantities.keySet());

        for (Inventory stock : stocks) {
            int toAddBack = itemQuantities.get(stock.getFoodId());
            stock.setAvailableAmount(stock.getAvailableAmount() + toAddBack);
        }
        inventoryRepository.saveAll(stocks);
    }

    public Order updateStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        order.setOrderStatus(newStatus);
        return orderRepository.save(order);
    }

    public Page<Order> getCustomerOrders(String customerId, int page, int size) {
        int safePage = Math.max(paginationConfig.getSafePage(), page);
        int safeSize = Math.max(1, Math.min(size, paginationConfig.getSafeSize()));

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("orderDate").descending());
        return orderRepository.findByCustomer_CustomerId(customerId, pageable);
    }
}