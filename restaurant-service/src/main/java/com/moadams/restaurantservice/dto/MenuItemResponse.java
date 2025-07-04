package com.moadams.restaurantservice.dto;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Boolean available,
        Long restaurantId
) {}