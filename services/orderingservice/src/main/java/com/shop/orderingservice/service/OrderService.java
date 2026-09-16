package com.shop.orderingservice.service;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.shop.orderingservice.config.PaginationConfig;
import com.shop.orderingservice.dto.OrderEventDTO;
import com.shop.orderingservice.exception.OrderNotFoundException;
import com.shop.orderingservice.model.Customer;
import com.shop.orderingservice.model.Order;
import com.shop.orderingservice.model.OrderItem;
import com.shop.orderingservice.model.enums.OrderStatus;
import com.shop.orderingservice.protobuf.CustomerProto;
import com.shop.orderingservice.protobuf.OrderEventProto;
import com.shop.orderingservice.protobuf.OrderItemProto;
import com.shop.orderingservice.protobuf.PaymentRequestProto;
import com.shop.orderingservice.repo.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
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
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
    }

    public Order getOrderForUser(String orderId, Authentication authentication) {
        requireAuthenticated(authentication);
        Order order = findById(orderId);

        if (isAdmin(authentication)) {
            return order;
        }

        if (!authentication.getName().equals(order.getCustomer().getCustomerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "You do not have permission to access this order");
        }
        return order;
    }

    /**
     * Places an order using the authenticated gateway identity. The client cannot
     * choose the customer ID, item name, item price, total amount or order status.
     * Inventory is reserved in PostgreSQL in its own transaction, the order is
     * persisted in MongoDB, and payment is requested through RabbitMQ. These are
     * different resources, so there is intentionally no fake cross-resource ACID
     * transaction; compensation is used when a later step fails.
     */
    public Order processAndPlaceOrder(OrderEventDTO orderRequest, Authentication authentication) {
        requireUser(authentication);

        if (orderRequest == null) {
            throw new IllegalArgumentException("Order request is required");
        }

        String userId = authentication.getName();
        Order newOrder = new Order();

        Customer customer = new Customer();
        customer.setCustomerId(userId);
        if (orderRequest.getCustomer() != null) {
            customer.setName(trimToNull(orderRequest.getCustomer().getName()));
            customer.setEmail(trimToNull(orderRequest.getCustomer().getEmail()));
            customer.setPhoneNo(trimToNull(orderRequest.getCustomer().getPhoneNo()));
        }

        newOrder.setCustomer(customer);
        newOrder.setOrderStatus(OrderStatus.PENDING);
        newOrder.setOrderDate(LocalDateTime.now());

        // Locks and decrements inventory, while taking the server-side price snapshot.
        inventoryService.reserveAndPrice(newOrder, orderRequest.getItems());

        final Order savedOrder;
        try {
            savedOrder = orderRepository.save(newOrder);
        } catch (Exception e) {
            safelyReleaseInventory(newOrder);
            throw new IllegalStateException("Failed to create order", e);
        }

        PaymentRequestProto paymentRequest = PaymentRequestProto.newBuilder()
                .setOrderId(savedOrder.getOrderId())
                .setCustomerId(userId)
                .setAmount(savedOrder.getTotalAmount().toPlainString())
                .setPaymentMode(orderRequest.getMode())
                .build();

        try {
            rabbitTemplate.convertAndSend(exchangeName, requestRoutingKey, paymentRequest.toByteArray());
        } catch (Exception e) {
            // Payment was not submitted, therefore the reservation must be compensated.
            safelyReleaseInventory(savedOrder);
            safelyDeleteOrder(savedOrder.getOrderId());
            throw new IllegalStateException("Unable to submit payment request; order cancelled", e);
        }

        sendOrderNotification(savedOrder, "Order placed successfully and is awaiting payment.");
        return savedOrder;
    }

    public void handlePaymentResult(String orderId, boolean success) {
        Order order = findById(orderId);

        // Only PENDING orders can consume a payment result. This makes repeated
        // payment messages harmless.
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.info("Ignoring payment result for order {} in status {}", orderId, order.getOrderStatus());
            return;
        }

        if (success) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            Order saved = orderRepository.save(order);
            sendOrderNotification(saved, "Payment successful! Your order is confirmed.");
            return;
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        safelyReleaseInventory(saved);
        sendOrderNotification(saved, "Payment failed. Your order has been cancelled.");
    }

    public Order updateStatus(String orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Order status is required");
        }

        Order order = findById(orderId);
        OrderStatus current = order.getOrderStatus();

        if (current == newStatus) {
            return order;
        }

        validateStatusTransition(current, newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            Order saved = orderRepository.save(order);
            safelyReleaseInventory(saved);
            sendOrderNotification(saved, "Your order has been cancelled.");
            return saved;
        }

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);
        sendOrderNotification(saved, "Order status updated to " + newStatus + ".");
        return saved;
    }

    public Page<Order> getOrdersForCurrentUser(Authentication authentication, int page, int size) {
        requireAuthenticated(authentication);
        Pageable pageable = pageable(page, size);

        if (isAdmin(authentication)) {
            return orderRepository.findAll(pageable);
        }

        return orderRepository.findByCustomer_CustomerId(authentication.getName(), pageable);
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int configuredMax = paginationConfig.getSafeSize() > 0 ? paginationConfig.getSafeSize() : 100;
        int safeSize = Math.max(1, Math.min(size, configuredMax));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "orderDate"));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == null) {
            throw new IllegalStateException("Order has no current status");
        }

        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CANCELLED || next == OrderStatus.CONFIRMED;
            case CONFIRMED -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.OUT_FOR_DELIVERY || next == OrderStatus.CANCELLED;
            case OUT_FOR_DELIVERY -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid order status transition: " + current + " -> " + next);
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void requireUser(Authentication authentication) {
        requireAuthenticated(authentication);
        if (!authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only USER accounts can place orders");
        }
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Authentication is required");
        }
    }

    private void safelyReleaseInventory(Order order) {
        try {
            inventoryService.release(order);
        } catch (Exception e) {
            log.error("Inventory compensation failed for order {}. Manual reconciliation may be required.",
                    order.getOrderId(), e);
        }
    }

    private void safelyDeleteOrder(String orderId) {
        try {
            orderRepository.deleteById(orderId);
        } catch (Exception e) {
            log.error("Failed to delete cancelled order {} after payment publication failure", orderId, e);
        }
    }

    private void sendOrderNotification(Order order, String messageText) {
        try {
            var customer = CustomerProto.newBuilder()
                    .setName(order.getCustomer().getName() != null ? order.getCustomer().getName() : "Customer")
                    .setEmail(order.getCustomer().getEmail() != null ? order.getCustomer().getEmail() : "No Email")
                    .setPhoneNo(order.getCustomer().getPhoneNo() != null ? order.getCustomer().getPhoneNo() : "N/A")
                    .build();

            var event = OrderEventProto.newBuilder()
                    .setOrderId(order.getOrderId())
                    .setStatus(order.getOrderStatus().name())
                    .setMessage(messageText)
                    .setTotalAmount(order.getTotalAmount().toPlainString())
                    .setCustomer(customer);

            if (order.getOrderDetails() != null) {
                for (OrderItem item : order.getOrderDetails()) {
                    event.addItems(OrderItemProto.newBuilder()
                            .setName(item.getName())
                            .setQuantity(item.getQuantity())
                            .setPricePerUnit(item.getPricePerUnit().toPlainString())
                            .build());
                }
            }

            rabbitTemplate.convertAndSend(exchangeName, notificationRoutingKey, event.build().toByteArray());
        } catch (Exception e) {
            log.error("Failed to publish order notification for {}", order.getOrderId(), e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
