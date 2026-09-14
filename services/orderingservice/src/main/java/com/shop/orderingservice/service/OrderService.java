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

import com.shop.orderingservice.config.PaginationConfig;
import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.exception.InsufficientInventoryException;
import com.shop.orderingservice.exception.OrderNotFoundException;
import com.shop.orderingservice.exception.ResourceNotFoundException;
import com.shop.orderingservice.model.Customer;
import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.OrderItem;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.protobuf.PaymentRequestProto;
import com.shop.orderingservice.repo.InventoryRepository;
import com.shop.orderingservice.repo.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaginationConfig paginationConfig;
    
    @Value("${payment.exchange.key}")
    private String exchangeName;
    
    @Value("${payment.request.routing.key}")
    private String requestRoutingKey;
    
    @Value("${rabbitmq.routing.key}")
    private String notificationRoutingKey;

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
            List<OrderItem> items = orderRequest.getItems().stream().map(dto -> {
                // FIX: Use Local Inventory Database instead of synchronous HTTP call!
                Inventory inventory = inventoryRepository.findById(dto.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food ID " + dto.getFoodId() + " not found in local inventory"));
                
                OrderItem item = new OrderItem();
                item.setName(inventory.getFoodName());
                item.setQuantity(dto.getQuantity());
                item.setPricePerUnit(inventory.getFoodPrice());
                item.setFoodId(dto.getFoodId());
                return item;
            }).collect(Collectors.toList());
            
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
            //revertInventory(newOrder); // Roll-back PostgresDB if MongoDB fails
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
        
        sendOrderNotification(savedOrder, "Order placed successfully and is awaiting payment.");
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

    @Transactional
    public void revertInventory(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return;
        }

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

    @Transactional
    public void handlePaymentResult(String orderId, boolean success) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return;
        }

        if (success) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            sendOrderNotification(order, "Payment successful! Your order is confirmed."); // NEW
        } else {
            revertInventory(order);
            order.setOrderStatus(OrderStatus.CANCELLED);
            sendOrderNotification(order, "Payment failed. Your order has been cancelled."); // NEW
        }
        orderRepository.save(order);
    }
    
    private void sendOrderNotification(Order order, String messageText) {
        try {
            com.shop.orderingservice.protobuf.CustomerProto customerProto = com.shop.orderingservice.protobuf.CustomerProto.newBuilder()
                    .setName(order.getCustomer().getName() != null ? order.getCustomer().getName() : "Customer")
                    .setEmail(order.getCustomer().getEmail() != null ? order.getCustomer().getEmail() : "No Email")
                    .setPhoneNo(order.getCustomer().getPhoneNo() != null ? order.getCustomer().getPhoneNo() : "N/A")
                    .build();

            com.shop.orderingservice.protobuf.OrderEventProto.Builder eventBuilder = com.shop.orderingservice.protobuf.OrderEventProto.newBuilder()
                    .setOrderId(order.getOrderId())
                    .setStatus(order.getOrderStatus().name())
                    .setMessage(messageText)
                    .setTotalAmount(order.getTotalAmount().toString())
                    .setCustomer(customerProto);

            if (order.getOrderDetails() != null) {
                for (OrderItem item : order.getOrderDetails()) {
                    eventBuilder.addItems(com.shop.orderingservice.protobuf.OrderItemProto.newBuilder()
                            .setName(item.getName())
                            .setQuantity(item.getQuantity())
                            .setPricePerUnit(item.getPricePerUnit().toString())
                            .build());
                }
            }

            rabbitTemplate.convertAndSend(exchangeName, notificationRoutingKey, eventBuilder.build().toByteArray());
            log.info("Successfully published Order Notification for Order ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send Order Notification to RabbitMQ", e);
        }
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