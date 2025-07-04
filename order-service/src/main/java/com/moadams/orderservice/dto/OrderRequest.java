package com.moadams.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "Restaurant ID is required")
        String restaurantId,

        @NotBlank(message = "Delivery address is required")
        String deliveryAddress,

        @NotEmpty(message = "Order must contain at least one item")
        List<OrderItemRequest> orderItems

) {}