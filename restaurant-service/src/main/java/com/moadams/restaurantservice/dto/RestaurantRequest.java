package com.moadams.restaurantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestaurantRequest(
        @NotBlank(message = "Restaurant name cannot be blank")
        String name,

        @NotBlank(message = "Address cannot be blank")
        String address,

        @NotBlank(message = "Phone number cannot be blank")
        @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
        String phone
) {
}