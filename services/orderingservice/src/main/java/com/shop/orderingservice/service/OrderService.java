package com.shop.orderingservice.service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.orderingservice.client.PaymentClient;
import com.shop.orderingservice.dto.PaymentResponse;
import com.shop.orderingservice.model.Inventory;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.OrderItem;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.repo.InventoryRepository;
import com.shop.orderingservice.repo.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final PaymentClient paymentClient;
	private final InventoryRepository inventoryRepository;

	/**
	 * 
	 * @param orderId : String ID
	 * @return : Order Details
	 */
	public Order findById(String orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
	}

	/**
	 * 
	 * @param newOrder : Order
	 * @return : new Order
	 */
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
		    revertInventory(newOrder); // Rollback Postgres if Mongo fails
		    throw new RuntimeException("Failed to save order to database", e);
		}

		// 2. PROCESS PAYMENT
		PaymentResponse response;
		try {
			response = paymentClient.processPayment(newOrder.getCustomerDetails().getCustomerId(),
					newOrder.getTotalAmount());
		} catch (Exception e) {
			// Handle network timeouts/errors
			revertInventory(newOrder);
			savedOrder.setOrderStatus(OrderStatus.CANCELLED);
			return orderRepository.save(savedOrder);
		}

		// 3. HANDLE OUTCOME
		if (response.isSuccess()) {
			savedOrder.setOrderStatus(OrderStatus.CONFIRMED);
			return orderRepository.save(savedOrder);
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

//	public Page<Order> getCustomerOrders(String customerId, int page, int size) {
//		int safePage = Math.max(0, page);
//		int safeSize = Math.max(1, Math.min(size, 100));
//
//		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("orderDate").descending());
//		return orderRepository.findByCustomer_CustomerId(customerId, pageable);
//	}
}
