package com.moadams.orderservice.service;

import com.moadams.orderservice.dto.*;
import com.moadams.orderservice.event.OrderItemDetails;
import com.moadams.orderservice.event.OrderPlacedEvent;
import com.moadams.orderservice.exception.ResourceNotFoundException;
import com.moadams.orderservice.exception.UnauthorizedAccessException;
import com.moadams.orderservice.model.Order;
import com.moadams.orderservice.model.OrderItem;
import com.moadams.orderservice.model.enums.OrderStatus;
import com.moadams.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Value("${restaurant.service.url}")
    private String restaurantServiceUrl;

    private static final String ORDER_EVENTS_TOPIC = "order-events-topic";


    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }
        return (String) authentication.getPrincipal();
    }

    @Override
    public String createOrder(OrderRequest orderRequest) {
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

        log.info("Calling restaurant-service for restaurant ID: {} with Circuit Breaker.", orderRequest.restaurantId());
        CustomApiResponse<RestaurantServiceResponse> restaurantApiResponse = getRestaurantServiceDetails(orderRequest.restaurantId())
                .block();

        if (restaurantApiResponse == null || restaurantApiResponse.data() == null) {
            log.error("Failed to retrieve restaurant details or fallback returned null for restaurantId: {}", orderRequest.restaurantId());
            throw new RuntimeException("Cannot create order: Restaurant details unavailable due to service issue.");
        }
        order.setRestaurantName(restaurantApiResponse.data().name());

        List<String> menuItemIds = orderRequest.orderItems().stream()
                .map(OrderItemRequest::menuItemId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, MenuItemServiceResponse> menuItemsMap = new HashMap<>();

        WebClient restaurantWebClient = webClientBuilder.baseUrl(restaurantServiceUrl).build();

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


        List<OrderItemDetails> itemDetails = savedOrder.getOrderItems().stream()
                .map(item -> new OrderItemDetails(
                        item.getMenuItemId(),
                        item.getMenuItemName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());

        OrderPlacedEvent event = new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getUserEmail(),
                savedOrder.getRestaurantId(),
                savedOrder.getRestaurantName(),
                savedOrder.getTotalAmount(),
                savedOrder.getDeliveryAddress(),
                savedOrder.getOrderDate(),
                itemDetails
        );


        kafkaTemplate.send(ORDER_EVENTS_TOPIC, event.orderId(), event);
        log.info("OrderPlacedEvent published for Order ID: {}", savedOrder.getId());

        return "Order created with ID: " + savedOrder.getId();
    }

    @CircuitBreaker(name = "restaurantServiceCircuitBreaker", fallbackMethod = "getRestaurantFallback")
    public Mono<CustomApiResponse<RestaurantServiceResponse>> getRestaurantServiceDetails(String restaurantId) {
        log.info("Attempting to get restaurant details from restaurant-service for ID: {}", restaurantId);
        return webClientBuilder.baseUrl(restaurantServiceUrl).build()
                .get()
                .uri("/api/restaurants/{restaurantId}", restaurantId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error response from restaurant-service (status {}): {}", clientResponse.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Restaurant service returned error: " + errorBody));
                        }))
                .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<RestaurantServiceResponse>>() {})
                .doOnError(e -> log.error("WebClient call to restaurant-service failed: {}", e.getMessage()));
    }

    public Mono<CustomApiResponse<RestaurantServiceResponse>> getRestaurantFallback(String restaurantId, Throwable t) {
        log.warn("Fallback triggered for getRestaurantServiceDetails for restaurantId: {}. Reason: {}", restaurantId, t.getMessage());

        List<MenuItemServiceResponse> fallbackMenuItems = List.of(
                new MenuItemServiceResponse("fallback-item-1", "Unavailable Item 1", BigDecimal.ZERO),
                new MenuItemServiceResponse("fallback-item-2", "Unavailable Item 2", BigDecimal.ZERO)
        );

        RestaurantServiceResponse fallbackRestaurant = new RestaurantServiceResponse(
                restaurantId,
                "Fallback Restaurant Name (Service Unavailable)",
                "Fallback Address (Service Issue)",
                "Fallback Contact (Service Issue)"
        );

        CustomApiResponse<RestaurantServiceResponse> fallbackApiResponse = new CustomApiResponse<>(
                false,
                "Restaurant service is currently unavailable. Using fallback data.",
                503,
                fallbackRestaurant
        );

        return Mono.just(fallbackApiResponse);
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

}