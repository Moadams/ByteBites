package com.moadams.restaurantservice.controller;

import com.moadams.restaurantservice.dto.CustomApiResponse;
import com.moadams.restaurantservice.dto.RestaurantRequest;
import com.moadams.restaurantservice.dto.RestaurantResponse;
import com.moadams.restaurantservice.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /**
     * Creates a new restaurant.
     * Accessible by: ROLE_RESTAURANT_OWNER (owner email taken from token)
     * Request body: RestaurantRequest DTO
     * Response: CustomApiResponse<RestaurantResponse>
     */
    @PostMapping
    public ResponseEntity<CustomApiResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody RestaurantRequest restaurantRequest) {

        RestaurantResponse createdRestaurant = restaurantService.createRestaurant(restaurantRequest);

        CustomApiResponse<RestaurantResponse> response = new CustomApiResponse<>(
                true,
                "Restaurant created successfully.",
                HttpStatus.CREATED.value(),
                createdRestaurant
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves all restaurants.
     * Accessible by: Any authenticated user (e.g., ROLE_USER, ROLE_RESTAURANT_OWNER, ROLE_ADMIN)
     * Response: CustomApiResponse<List<RestaurantResponse>>
     */
    @GetMapping
    public ResponseEntity<CustomApiResponse<List<RestaurantResponse>>> getAllRestaurants() {

        List<RestaurantResponse> restaurants = restaurantService.getAllRestaurants();

        CustomApiResponse<List<RestaurantResponse>> response = new CustomApiResponse<>(
                true,
                "All restaurants retrieved successfully.",
                HttpStatus.OK.value(),
                restaurants
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves restaurants owned by the current authenticated user.
     * Accessible by: ROLE_RESTAURANT_OWNER
     * Response: CustomApiResponse<List<RestaurantResponse>>
     */
    @GetMapping("/my")
    public ResponseEntity<CustomApiResponse<List<RestaurantResponse>>> getMyRestaurants() {

        List<RestaurantResponse> myRestaurants = restaurantService.getMyRestaurants();

        CustomApiResponse<List<RestaurantResponse>> response = new CustomApiResponse<>(
                true,
                "Your restaurants retrieved successfully.",
                HttpStatus.OK.value(),
                myRestaurants
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    /**
     * Retrieves a restaurant by its ID.
     * Accessible by: Any authenticated user
     * Path variable: restaurantId
     * Response: CustomApiResponse<RestaurantResponse>
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomApiResponse<RestaurantResponse>> getRestaurantById(@PathVariable Long id) {

        RestaurantResponse restaurant = restaurantService.getRestaurantById(id);

        CustomApiResponse<RestaurantResponse> response = new CustomApiResponse<>(
                true,
                "Restaurant retrieved successfully.",
                HttpStatus.OK.value(),
                restaurant
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates an existing restaurant by ID.
     * Accessible by: ROLE_RESTAURANT_OWNER (only if they own the restaurant)
     * Path variable: restaurantId
     * Request body: RestaurantRequest DTO
     * Response: CustomApiResponse<RestaurantResponse>
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomApiResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest restaurantRequest) {

        RestaurantResponse updatedRestaurant = restaurantService.updateRestaurant(id, restaurantRequest);

        CustomApiResponse<RestaurantResponse> response = new CustomApiResponse<>(
                true,
                "Restaurant updated successfully.",
                HttpStatus.OK.value(),
                updatedRestaurant
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Deletes a restaurant by its ID.
     * Accessible by: ROLE_RESTAURANT_OWNER (only if they own the restaurant)
     * Path variable: restaurantId
     * Response: CustomApiResponse<Void> (or just a message)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CustomApiResponse<Void>> deleteRestaurant(@PathVariable Long id) {

        restaurantService.deleteRestaurant(id);

        CustomApiResponse<Void> response = new CustomApiResponse<>(
                true,
                "Restaurant deleted successfully.",
                HttpStatus.NO_CONTENT.value(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}