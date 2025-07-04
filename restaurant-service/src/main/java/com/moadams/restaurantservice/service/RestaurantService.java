package com.moadams.restaurantservice.service;

import com.moadams.restaurantservice.dto.RestaurantRequest;
import com.moadams.restaurantservice.dto.RestaurantResponse;
import com.moadams.restaurantservice.exception.ResourceNotFoundException;
import com.moadams.restaurantservice.exception.UnauthorizedAccessException;
import com.moadams.restaurantservice.model.Restaurant;
import com.moadams.restaurantservice.repository.RestaurantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }
        return (String) authentication.getPrincipal();
    }

    private void checkRestaurantOwnership(Restaurant restaurant) {
        String currentUserEmail = getCurrentUserEmail();
        if (!restaurant.getOwnerEmail().equals(currentUserEmail)) {
            throw new UnauthorizedAccessException("You are not authorized to manage this restaurant.");
        }
    }


    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        String ownerEmail = getCurrentUserEmail();

        Restaurant restaurant = Restaurant.builder()
                .name(request.name())
                .address(request.address())
                .phone(request.phone())
                .ownerEmail(ownerEmail)
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(savedRestaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::mapToRestaurantResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return mapToRestaurantResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getMyRestaurants() {
        String ownerEmail = getCurrentUserEmail();
        return restaurantRepository.findByOwnerEmail(ownerEmail).stream()
                .map(this::mapToRestaurantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request) {
        Restaurant existingRestaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));

        checkRestaurantOwnership(existingRestaurant);

        existingRestaurant.setName(request.name());
        existingRestaurant.setAddress(request.address());
        existingRestaurant.setPhone(request.phone());

        Restaurant updatedRestaurant = restaurantRepository.save(existingRestaurant);
        return mapToRestaurantResponse(updatedRestaurant);
    }

    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));

        checkRestaurantOwnership(restaurant);

        restaurantRepository.delete(restaurant);
    }

    private RestaurantResponse mapToRestaurantResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getOwnerEmail()
        );
    }
}