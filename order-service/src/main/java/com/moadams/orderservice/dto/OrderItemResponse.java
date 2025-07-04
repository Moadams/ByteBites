package com.moadams.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String menuItemId,
        String menuItemName,
        int quantity,
        BigDecimal price
) {}