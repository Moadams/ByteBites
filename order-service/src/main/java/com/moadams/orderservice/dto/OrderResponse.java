package com.moadams.orderservice.dto;

import com.moadams.orderservice.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String userId,
        String userEmail,
        String restaurantId,
        String restaurantName,
        List<OrderItemResponse> orderItems,
        BigDecimal totalAmount,
        OrderStatus status,
        String deliveryAddress,
        LocalDateTime orderDate,
        LocalDateTime lastUpdated
) {}