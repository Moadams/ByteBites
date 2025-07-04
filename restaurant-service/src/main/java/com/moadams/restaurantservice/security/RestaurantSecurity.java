package com.moadams.restaurantservice.security;

import com.moadams.restaurantservice.model.Restaurant;
import com.moadams.restaurantservice.repository.RestaurantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("restaurantSecurity")
public class RestaurantSecurity {

    private final RestaurantRepository restaurantRepository;

    public RestaurantSecurity(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Checks if the currently authenticated user is the owner of the given restaurant.
     * @param restaurantId The ID of the restaurant to check ownership for.
     * @return true if the current user is the owner, false otherwise.
     */
    public boolean isOwner(String restaurantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String currentUserEmail = authentication.getName();

        Optional<Restaurant> restaurantOptional = restaurantRepository.findById(Long.valueOf(restaurantId));

        return restaurantOptional.map(restaurant ->
                        restaurant.getOwnerEmail() != null && restaurant.getOwnerEmail().equals(currentUserEmail))
                .orElse(false);
    }
}