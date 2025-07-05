package com.moadams.orderservice.dto;

public record RestaurantServiceResponse(
        String id,
        String name,
        String address,
        String contactInfo
) {}