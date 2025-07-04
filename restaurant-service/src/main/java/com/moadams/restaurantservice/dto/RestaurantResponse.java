package com.moadams.restaurantservice.dto;

public record RestaurantResponse(
        Long id,
        String name,
        String address,
        String phone,
        String ownerEmail
) {

}