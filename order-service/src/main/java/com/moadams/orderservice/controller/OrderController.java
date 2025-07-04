package com.moadams.orderservice.controller;

import com.moadams.orderservice.dto.CustomApiResponse;
import com.moadams.orderservice.dto.OrderRequest;
import com.moadams.orderservice.dto.OrderResponse;
import com.moadams.orderservice.dto.OrderStatusUpdateRequest;
import com.moadams.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CustomApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-Auth-User-Id") String userId,
            @RequestHeader("X-Auth-User-Email") String userEmail,
            @Valid @RequestBody OrderRequest orderRequest) {
        OrderResponse createdOrder = orderService.createOrder(userId, userEmail, orderRequest);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order created successfully", HttpStatus.CREATED.value(), createdOrder),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<CustomApiResponse<OrderResponse>> getOrderById(@PathVariable String orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order retrieved successfully", HttpStatus.OK.value(), order),
                HttpStatus.OK
        );
    }

    public ResponseEntity<CustomApiResponse<List<OrderResponse>>> getOrdersByUserId(
            @PathVariable String userId) {
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Orders retrieved successfully", HttpStatus.OK.value(), orders),
                HttpStatus.OK
        );
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<CustomApiResponse<List<OrderResponse>>> getOrdersByRestaurantId(
            @PathVariable String restaurantId) {
        List<OrderResponse> orders = orderService.getOrdersByRestaurantId(restaurantId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Orders retrieved for restaurant successfully", HttpStatus.OK.value(), orders),
                HttpStatus.OK
        );
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<CustomApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateRequest statusUpdateRequest) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(orderId, statusUpdateRequest);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order status updated successfully", HttpStatus.OK.value(), updatedOrder),
                HttpStatus.OK
        );
    }

    public ResponseEntity<CustomApiResponse<Void>> cancelOrder(@PathVariable String orderId) {
        orderService.cancelOrder(orderId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order cancelled successfully", HttpStatus.NO_CONTENT.value(), null),
                HttpStatus.NO_CONTENT
        );
    }
}