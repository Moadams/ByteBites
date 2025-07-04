package com.moadams.orderservice.service;

import com.moadams.orderservice.dto.OrderRequest;
import com.moadams.orderservice.dto.OrderResponse;
import com.moadams.orderservice.dto.OrderStatusUpdateRequest;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(String userId, String userEmail, OrderRequest orderRequest);
    OrderResponse getOrderById(String orderId);
    List<OrderResponse> getOrdersByUserId(String userId);
    List<OrderResponse> getOrdersByRestaurantId(String restaurantId);
    OrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest statusUpdateRequest);
    void cancelOrder(String orderId);
}