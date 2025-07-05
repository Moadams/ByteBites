package com.moadams.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public record OrderPlacedEvent(
        String orderId,
        String userEmail,
        String restaurantId,
        String restaurantName,
        BigDecimal totalAmount,
        String deliveryAddress,
        LocalDateTime orderDate,
        List<OrderItemDetails> orderItems
) {}