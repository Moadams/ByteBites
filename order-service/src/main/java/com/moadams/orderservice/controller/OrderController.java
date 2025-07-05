package com.moadams.orderservice.controller;

import com.moadams.orderservice.dto.*;
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

    @GetMapping
    public ResponseEntity getOrders(){
        return ResponseEntity.ok("orders fetched");
    }


    @PostMapping
    public ResponseEntity<CustomApiResponse<String>> createOrder(
            @Valid @RequestBody OrderRequest orderRequest) {
        String createdOrder = orderService.createOrder(orderRequest);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order created successfully", HttpStatus.CREATED.value(), createdOrder),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<CustomApiResponse<OrderSummaryResponse>> getOrderById(@PathVariable String orderId) {
        OrderSummaryResponse order = orderService.getOrderById(orderId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order retrieved successfully", HttpStatus.OK.value(), order),
                HttpStatus.OK
        );
    }

    public ResponseEntity<CustomApiResponse<List<OrderSummaryResponse>>> getOrdersByUserId(
            @PathVariable String userEmail) {
        List<OrderSummaryResponse> orders = orderService.getOrdersByUserEmail(userEmail);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Orders retrieved successfully", HttpStatus.OK.value(), orders),
                HttpStatus.OK
        );
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<CustomApiResponse<List<OrderSummaryResponse>>> getOrdersByRestaurantId(
            @PathVariable String restaurantId) {
        List<OrderSummaryResponse> orders = orderService.getOrdersByRestaurantId(restaurantId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Orders retrieved for restaurant successfully", HttpStatus.OK.value(), orders),
                HttpStatus.OK
        );
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<CustomApiResponse<OrderSummaryResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateRequest statusUpdateRequest) {
        OrderSummaryResponse updatedOrder = orderService.updateOrderStatus(orderId, statusUpdateRequest);
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