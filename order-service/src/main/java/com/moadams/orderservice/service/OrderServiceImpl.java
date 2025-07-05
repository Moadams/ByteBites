package com.moadams.orderservice.service;

import com.moadams.orderservice.dto.*;
import com.moadams.orderservice.exception.ResourceNotFoundException;
import com.moadams.orderservice.exception.UnauthorizedAccessException;
import com.moadams.orderservice.model.Order;
import com.moadams.orderservice.model.OrderItem;
import com.moadams.orderservice.model.enums.OrderStatus;
import com.moadams.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${restaurant.service.url}")
    private String restaurantServiceUrl;

    public OrderServiceImpl(OrderRepository orderRepository, WebClient.Builder webClientBuilder) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
    }


    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }
        return (String) authentication.getPrincipal();
    }

    @Override
    public String createOrder(OrderRequest orderRequest) {
        // Validation
        if (orderRequest.orderItems() == null || orderRequest.orderItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .userEmail(getCurrentUserEmail())
                .restaurantId(orderRequest.restaurantId())
                .deliveryAddress(orderRequest.deliveryAddress())
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();

        WebClient restaurantWebClient = webClientBuilder.baseUrl(restaurantServiceUrl).build();

        // Fetch restaurant info
        CustomApiResponse<RestaurantServiceResponse> restaurantApiResponse = restaurantWebClient.get()
                .uri("/api/restaurants/{restaurantId}", orderRequest.restaurantId())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new ResourceNotFoundException(
                                        "Restaurant not found with ID: " + orderRequest.restaurantId()))))
                .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<RestaurantServiceResponse>>() {})
                .block();

        if (restaurantApiResponse == null || restaurantApiResponse.data() == null) {
            throw new ResourceNotFoundException("Restaurant not found with ID: " + orderRequest.restaurantId());
        }
        order.setRestaurantName(restaurantApiResponse.data().name());

        // Collect all menu item IDs for batch processing
        List<String> menuItemIds = orderRequest.orderItems().stream()
                .map(OrderItemRequest::menuItemId)
                .distinct()
                .collect(Collectors.toList());

        // Create a map to store menu items (in a real implementation, you'd make a batch request)
        Map<String, MenuItemServiceResponse> menuItemsMap = new HashMap<>();

        // For now, we'll still need individual requests, but we could optimize this with a batch endpoint
        for (String menuItemId : menuItemIds) {
            CustomApiResponse<MenuItemServiceResponse> menuItemApiResponse = restaurantWebClient.get()
                    .uri("/api/restaurants/{restaurantId}/menu-items/{menuItemId}",
                            orderRequest.restaurantId(), menuItemId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new ResourceNotFoundException(
                                            "Menu item not found with ID: " + menuItemId))))
                    .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<MenuItemServiceResponse>>() {})
                    .block();

            if (menuItemApiResponse != null && menuItemApiResponse.data() != null) {
                menuItemsMap.put(menuItemId, menuItemApiResponse.data());
            }
        }

        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;

        // Process order items
        for (OrderItemRequest itemRequest : orderRequest.orderItems()) {
            if (itemRequest.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for menu item: " + itemRequest.menuItemId());
            }

            MenuItemServiceResponse menuItem = menuItemsMap.get(itemRequest.menuItemId());
            if (menuItem == null) {
                throw new ResourceNotFoundException("Menu item not found with ID: " + itemRequest.menuItemId());
            }

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(menuItem.id())
                    .menuItemName(menuItem.name())
                    .quantity(itemRequest.quantity())
                    .price(menuItem.price())
                    .build();

            order.addOrderItem(orderItem);

            calculatedTotalAmount = calculatedTotalAmount.add(
                    menuItem.price().multiply(BigDecimal.valueOf(itemRequest.quantity()))
            );
        }

        order.setTotalAmount(calculatedTotalAmount);

        Order savedOrder = orderRepository.save(order);
        return "Order created with ID: " + savedOrder.getId();
    }

    @Override
    public OrderSummaryResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return convertToDto(order);
    }

    @Override
    public List<OrderSummaryResponse> getOrdersByUserEmail(String userEmail) {
        List<Order> orders = orderRepository.findByUserEmail(userEmail);
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<OrderSummaryResponse> getOrdersByRestaurantId(String restaurantId) {
        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public OrderSummaryResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest statusUpdateRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(statusUpdateRequest.newStatus());
        order.setLastUpdated(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        return convertToDto(updatedOrder);
    }

    @Override
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an order that is already DELIVERED or CANCELLED.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastUpdated(LocalDateTime.now());
        orderRepository.save(order);
    }

    private OrderSummaryResponse convertToDto(Order order) {

        return new OrderSummaryResponse(
                order.getId(),
                order.getUserEmail(),
                order.getRestaurantName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getDeliveryAddress(),
                order.getOrderDate()
        );
    }

    private OrderItemResponse convertToDto(OrderItem item) {
        return new OrderItemResponse(
                item.getMenuItemId(),
                item.getMenuItemName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}