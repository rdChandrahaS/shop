package com.shop.orderingservice.service;

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

import com.shop.orderingservice.client.PaymentClient;
import com.shop.orderingservice.dto.CustomerDTO;
import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.dto.OrderItemDTO;
import com.shop.orderingservice.dto.PaymentResponse;
import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.OrderItem;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.protobuf.CustomerProto;
import com.shop.orderingservice.protobuf.OrderEventProto;
import com.shop.orderingservice.protobuf.OrderItemProto;
import com.shop.orderingservice.repo.InventoryRepository;
import com.shop.orderingservice.repo.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final PaymentClient paymentClient;
	private final InventoryRepository inventoryRepository;
	private final RabbitTemplate rabbitTemplate;
	
	@Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing.key}")
    private String routingKey;

	public Order findById(String orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(
						() -> new RuntimeException("Order not found with ID: " + orderId)
				);
	}

	
	@Transactional
	public Order processAndPlaceOrder(Order newOrder) {
		
		// 1. DEDUCT INVENTORY (Fast Transaction)
		deductInventory(newOrder);

		// Save order as pending
		newOrder.setOrderStatus(OrderStatus.PENDING);
		newOrder.setOrderDate(LocalDateTime.now());
		Order savedOrder;
		
		try {
		    savedOrder = orderRepository.save(newOrder);
		} catch (Exception e) {
		    revertInventory(newOrder); // Roll-back PostgresDB if MongoDB fails
		    throw new RuntimeException("Failed to save order to database", e);
		}

		// 2. PROCESS PAYMENT
		PaymentResponse response;
		try {
			response = paymentClient.processPayment(
										newOrder.getCustomerDetails().getCustomerId(),
										newOrder.getTotalAmount()
									);
		} catch (Exception e) {
			// Handle network timeouts/errors
			revertInventory(newOrder);
			savedOrder.setOrderStatus(OrderStatus.CANCELLED);
			return orderRepository.save(savedOrder);
		}

		// 3. HANDLE OUTCOME
		if (response.isSuccess()) {
			savedOrder.setOrderStatus(OrderStatus.CONFIRMED);
			Order finalOrder = orderRepository.save(savedOrder);
			
			// Build the Protobuf Customer
		    CustomerProto customerProto = CustomerProto.newBuilder()
										            .setName(finalOrder.getCustomerDetails().getName())
										            .setEmail(finalOrder.getCustomerDetails().getEmail())
										            .setPhoneNo(finalOrder.getCustomerDetails().getPhoneNo())
										            .build();

		    // Build the Protobuf OrderEvent
		    OrderEventProto.Builder eventBuilder = OrderEventProto.newBuilder()
													            .setOrderId(finalOrder.getOrderId())
													            .setStatus(finalOrder.getOrderStatus().name())
													            .setMessage("Your payment was successful!")
													            .setTotalAmount(finalOrder.getTotalAmount())
													            .setCustomer(customerProto);
		 // Add each item to the repeated items list
		    finalOrder.getOrderDetails().forEach(item -> {
		        eventBuilder.addItems(
		            OrderItemProto.newBuilder()
		                .setName(item.getName())
		                .setQuantity(item.getQuantity())
		                .setPricePerUnit(item.getPricePerUnit())
		                .build()
		        );
		    });
			
			
		    OrderEventProto event = eventBuilder.build();
			
		    rabbitTemplate.convertAndSend(exchangeName, routingKey, event.toByteArray());
		    
		    return finalOrder;
		} else {
			// Compensation: Put the stock back manually!
			revertInventory(newOrder);
			savedOrder.setOrderStatus(OrderStatus.CANCELLED);
			return orderRepository.save(savedOrder);
		}
	}

	public void deductInventory(Order order) {
		if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
	        throw new RuntimeException("Order details cannot be empty!");
	    }
		Map<Long, Integer> itemQuantities = order.getOrderDetails().stream()
				.collect(Collectors.groupingBy(OrderItem::getFoodId, Collectors.summingInt(OrderItem::getQuantity)));

		List<Inventory> stocks = inventoryRepository.findAllByIdInForUpdate(itemQuantities.keySet());

		if (stocks.size() != itemQuantities.size()) {
			throw new RuntimeException("Database mismatch");
		}

		for (Inventory stock : stocks) {
			int required = itemQuantities.get(stock.getFoodId());
			if (stock.getAvailableAmount() < required) {
				throw new RuntimeException("Out of stock: " + stock.getFoodName());
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
				.orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
		order.setOrderStatus(newStatus);
		return orderRepository.save(order);
	}

	public Page<Order> getCustomerOrders(String customerId, int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, Math.min(size, 100));

		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("orderDate").descending());
		return orderRepository.findByCustomer_CustomerId(customerId, pageable);
	}
}
