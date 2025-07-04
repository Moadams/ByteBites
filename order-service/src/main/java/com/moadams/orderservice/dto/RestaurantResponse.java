package com.moadams.orderservice.dto;

public record RestaurantResponse(
        String id,
        String name,
        String address,
        String contactInfo
) {}