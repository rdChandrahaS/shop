package com.sweetshop.orderingservices.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sweetshop.orderingservices.client.PaymentClient;
import com.sweetshop.orderingservices.dto.PaymentResponse;
import com.sweetshop.orderingservices.model.Food;
import com.sweetshop.orderingservices.model.Order;
import com.sweetshop.orderingservices.model.OrderItem;
import com.sweetshop.orderingservices.model.enums.OrderStatus;
import com.sweetshop.orderingservices.repository.FoodRepository;
import com.sweetshop.orderingservices.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final FoodRepository foodRepository;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository, FoodRepository foodRepository, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
        this.paymentClient = paymentClient;
    }

    public Order findById(Long orderId){
        Order order = orderRepository
                        .findById(orderId)
                        .orElseThrow(
                            ()-> new RuntimeException("Order not found with ID: " + orderId)
                        );
        return order;
    }

    @Transactional
    public Order processAndPlaceOrder(Order newOrder) {
        for(OrderItem orderItem : newOrder.getItems()){
            Food foodItem = foodRepository.findByIdForUpdate(
                orderItem
                .getFood()
                .getFoodID())
                .orElseThrow(
                    () -> new RuntimeException("Food Not Found"));
            
            if(foodItem.getAvailable() < orderItem.getQuantity()){
                throw new RuntimeException("Sorry, we only have " + foodItem.getAvailable() + " " + foodItem.getName() + " left!");
            }
            foodItem.setAvailable(foodItem.getAvailable() - orderItem.getQuantity());
            foodRepository.save(foodItem);
            orderItem.setOrder(newOrder);
        }
        PaymentResponse response = paymentClient.processPayment(newOrder.getCustomer().getId(), newOrder.getTotalAmount());

        if(response.isSuccess()){
            newOrder.setOrderStatus(OrderStatus.CONFIRMED);
            newOrder.setDate(LocalDateTime.now());
            return orderRepository.save(newOrder);
        }else{
            throw new RuntimeException("Process Failed");
        }
    }

    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository
                        .findById(orderId)
                        .orElseThrow(
                            () -> new RuntimeException("Order not found with ID: " + orderId)
                        );
        order.setOrderStatus(newStatus);
        return orderRepository.save(order);
    }

    public Page<Order> getCustomerOrders(Long customerId , int page , int size){
        Pageable pageable = PageRequest.of(page, size,Sort.by("date").descending());
        return orderRepository.findByCustomerId(customerId, pageable);
    }
}
