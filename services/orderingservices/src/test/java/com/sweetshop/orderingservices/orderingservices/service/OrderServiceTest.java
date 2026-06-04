package com.sweetshop.orderingservices.orderingservices.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sweetshop.orderingservices.client.PaymentClient;
import com.sweetshop.orderingservices.dto.PaymentResponse;
import com.sweetshop.orderingservices.model.Customer;
import com.sweetshop.orderingservices.model.Food;
import com.sweetshop.orderingservices.model.Order;
import com.sweetshop.orderingservices.model.OrderItem;
import com.sweetshop.orderingservices.model.enums.OrderStatus;
import com.sweetshop.orderingservices.repository.FoodRepository;
import com.sweetshop.orderingservices.repository.OrderRepository;
import com.sweetshop.orderingservices.service.OrderService;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Food testFood;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);

        testFood = new Food();
        testFood.setFoodID(100L);
        testFood.setName("Rasgulla");
        testFood.setAvailable(50); 

        OrderItem item = new OrderItem();
        item.setFood(testFood);
        item.setQuantity(5); 

        testOrder = new Order();
        testOrder.setCustomer(testCustomer);
        testOrder.setTotalAmount(250.0);
        testOrder.setItems(List.of(item));
    }

    @Test
    void processAndPlaceOrder_Success() {
        when(foodRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testFood));
        
        PaymentResponse successResponse = new PaymentResponse();
        successResponse.setSuccess(true);
        when(paymentClient.processPayment(1L, 250.0)).thenReturn(successResponse);
        
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order savedOrder = orderService.processAndPlaceOrder(testOrder);

        assertNotNull(savedOrder);
        assertEquals(OrderStatus.CONFIRMED, testOrder.getOrderStatus()); 
        assertEquals(45, testFood.getAvailable()); 
        
        verify(foodRepository, times(1)).save(testFood);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void processAndPlaceOrder_FailsWhenOutOfStock() {
        testFood.setAvailable(2); 
        when(foodRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testFood));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.processAndPlaceOrder(testOrder);
        });

        assertTrue(exception.getMessage().contains("Sorry, we only have 2 Rasgulla left!"));

        verify(paymentClient, never()).processPayment(anyLong(), anyDouble());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processAndPlaceOrder_FailsWhenPaymentRejected() {
        when(foodRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(testFood));
        
        PaymentResponse failedResponse = new PaymentResponse();
        failedResponse.setSuccess(false); 
        when(paymentClient.processPayment(1L, 250.0)).thenReturn(failedResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.processAndPlaceOrder(testOrder);
        });

        assertEquals("Payment Process Failed. Order Cancelled.", exception.getMessage());

        verify(orderRepository, never()).save(any());
    }
}