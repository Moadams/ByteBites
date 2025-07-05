package com.moadams.orderservice.service;

import com.moadams.orderservice.dto.OrderRequest;
import com.moadams.orderservice.dto.OrderStatusUpdateRequest;
import com.moadams.orderservice.dto.OrderSummaryResponse;

import java.util.List;

public interface OrderService {
    String createOrder(OrderRequest orderRequest);
    OrderSummaryResponse getOrderById(String orderId);
    List<OrderSummaryResponse> getOrdersByUserEmail(String userEmail);
    List<OrderSummaryResponse> getOrdersByRestaurantId(String restaurantId);
    OrderSummaryResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest statusUpdateRequest);
    void cancelOrder(String orderId);
}