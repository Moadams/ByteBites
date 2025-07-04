package com.moadams.orderservice.service;

import com.moadams.orderservice.dto.CustomApiResponse;
import com.moadams.orderservice.dto.MenuItemServiceResponse;
import com.moadams.orderservice.dto.OrderItemRequest;
import com.moadams.orderservice.dto.OrderItemResponse;
import com.moadams.orderservice.dto.OrderRequest;
import com.moadams.orderservice.dto.OrderResponse;
import com.moadams.orderservice.dto.OrderStatusUpdateRequest;
import com.moadams.orderservice.dto.RestaurantResponse;
import com.moadams.orderservice.exception.ResourceNotFoundException;
import com.moadams.orderservice.model.Order;
import com.moadams.orderservice.model.OrderItem;
import com.moadams.orderservice.model.enums.OrderStatus;
import com.moadams.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public OrderResponse createOrder(String userId, String userEmail, OrderRequest orderRequest) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setUserEmail(userEmail);
        order.setRestaurantId(orderRequest.restaurantId());
        order.setDeliveryAddress(orderRequest.deliveryAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setLastUpdated(LocalDateTime.now());

        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;

        WebClient restaurantWebClient = webClientBuilder.baseUrl(restaurantServiceUrl).build();

        CustomApiResponse<RestaurantResponse> restaurantApiResponse = restaurantWebClient.get()
                .uri("/api/restaurants/{restaurantId}", orderRequest.restaurantId())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new RuntimeException("Error fetching restaurant ID " + orderRequest.restaurantId() + ": " + errorBody))
                        )
                )
                .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<RestaurantResponse>>() {})
                .block();

        if (restaurantApiResponse == null || restaurantApiResponse.data() == null) {
            throw new ResourceNotFoundException("Restaurant not found with ID: " + orderRequest.restaurantId());
        }
        order.setRestaurantName(restaurantApiResponse.data().name());


        for (OrderItemRequest itemRequest : orderRequest.orderItems()) {
            CustomApiResponse<MenuItemServiceResponse> menuItemApiResponse = restaurantWebClient.get()
                    .uri("/api/restaurants/{restaurantId}/menu-items/{menuItemId}",
                            orderRequest.restaurantId(), itemRequest.menuItemId())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new RuntimeException("Error fetching menu item ID " + itemRequest.menuItemId() + " for restaurant " + orderRequest.restaurantId() + ": " + errorBody))
                            )
                    )
                    .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<MenuItemServiceResponse>>() {})
                    .block();

            MenuItemServiceResponse menuItem = null;
            if (menuItemApiResponse != null && menuItemApiResponse.data() != null) {
                menuItem = menuItemApiResponse.data();
            }

            if (menuItem == null) {
                throw new ResourceNotFoundException("Menu item not found with ID: " + itemRequest.menuItemId() + " in restaurant " + orderRequest.restaurantId());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(menuItem.id());
            orderItem.setMenuItemName(menuItem.name());
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setPrice(menuItem.price());

            order.addOrderItem(orderItem);

            calculatedTotalAmount = calculatedTotalAmount.add(menuItem.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(calculatedTotalAmount);

        Order savedOrder = orderRepository.save(order);
        return convertToDto(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return convertToDto(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByRestaurantId(String restaurantId) {
        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest statusUpdateRequest) {
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

    private OrderResponse convertToDto(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getRestaurantId(),
                order.getRestaurantName(),
                itemResponses,
                order.getTotalAmount(),
                order.getStatus(),
                order.getDeliveryAddress(),
                order.getOrderDate(),
                order.getLastUpdated()
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