package com.moadams.orderservice.dto;

import com.moadams.orderservice.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderSummaryResponse(String id,
                                   String userEmail,
                                   String restaurantName,
                                   BigDecimal totalAmount,
                                   OrderStatus status,
                                   String deliveryAddress,
                                   LocalDateTime orderDate) {
}
