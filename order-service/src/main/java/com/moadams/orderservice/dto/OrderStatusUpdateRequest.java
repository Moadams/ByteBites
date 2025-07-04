package com.moadams.orderservice.dto;

import com.moadams.orderservice.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "New status is required")
        OrderStatus newStatus
) {}