package com.moadams.restaurantservice.controller;

import com.moadams.restaurantservice.dto.*;
import com.moadams.restaurantservice.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN') or @restaurantSecurity.isOwner(#id)")
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
    @PreAuthorize("hasAnyRole('ADMIN') or @restaurantSecurity.isOwner(#id)")
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

    /**
     * Creates a new menu item for a specific restaurant.
     * Accessible by: ROLE_RESTAURANT_OWNER (must own the restaurant)
     * Path variable: restaurantId
     * Request body: MenuItemRequest DTO
     * Response: CustomApiResponse<MenuItemResponse>
     */
    @PostMapping("/{restaurantId}/menu-items")
    @PreAuthorize("hasAnyRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId)")
    public ResponseEntity<CustomApiResponse<MenuItemResponse>> createMenuItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemRequest menuItemRequest) {

        MenuItemResponse createdMenuItem = restaurantService.createMenuItem(restaurantId, menuItemRequest);

        CustomApiResponse<MenuItemResponse> response = new CustomApiResponse<>(
                true,
                "Menu item created successfully.",
                HttpStatus.CREATED.value(),
                createdMenuItem
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves all menu items for a specific restaurant.
     * Accessible by: Any authenticated user
     * Path variable: restaurantId
     * Response: CustomApiResponse<List<MenuItemResponse>>
     */
    @GetMapping("/{restaurantId}/menu-items")
    public ResponseEntity<CustomApiResponse<List<MenuItemResponse>>> getAllMenuItemsByRestaurant(
            @PathVariable Long restaurantId) {

        List<MenuItemResponse> menuItems = restaurantService.getAllMenuItemsByRestaurant(restaurantId);

        CustomApiResponse<List<MenuItemResponse>> response = new CustomApiResponse<>(
                true,
                "Menu items retrieved successfully for restaurant.",
                HttpStatus.OK.value(),
                menuItems
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves a specific menu item by its ID for a specific restaurant.
     * Accessible by: Any authenticated user
     * Path variables: restaurantId, menuItemId
     * Response: CustomApiResponse<MenuItemResponse>
     */
    @GetMapping("/{restaurantId}/menu-items/{menuItemId}")
    public ResponseEntity<CustomApiResponse<MenuItemResponse>> getMenuItemById(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId) {

        MenuItemResponse menuItem = restaurantService.getMenuItemById(restaurantId, menuItemId);

        CustomApiResponse<MenuItemResponse> response = new CustomApiResponse<>(
                true,
                "Menu item retrieved successfully.",
                HttpStatus.OK.value(),
                menuItem
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates an existing menu item for a specific restaurant.
     * Accessible by: ROLE_RESTAURANT_OWNER (must own the restaurant)
     * Path variables: restaurantId, menuItemId
     * Request body: MenuItemRequest DTO
     * Response: CustomApiResponse<MenuItemResponse>
     */
    @PutMapping("/{restaurantId}/menu-items/{menuItemId}")
    @PreAuthorize("hasAnyRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId)")
    public ResponseEntity<CustomApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @Valid @RequestBody MenuItemRequest menuItemRequest) {

        MenuItemResponse updatedMenuItem = restaurantService.updateMenuItem(restaurantId, menuItemId, menuItemRequest);

        CustomApiResponse<MenuItemResponse> response = new CustomApiResponse<>(
                true,
                "Menu item updated successfully.",
                HttpStatus.OK.value(),
                updatedMenuItem
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Deletes a menu item for a specific restaurant.
     * Accessible by: ROLE_RESTAURANT_OWNER (must own the restaurant)
     * Path variables: restaurantId, menuItemId
     * Response: CustomApiResponse<Void>
     */
    @DeleteMapping("/{restaurantId}/menu-items/{menuItemId}")
    @PreAuthorize("hasAnyRole('ADMIN') or @restaurantSecurity.isOwner(#restaurantId)")
    public ResponseEntity<CustomApiResponse<Void>> deleteMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId) {

        restaurantService.deleteMenuItem(restaurantId, menuItemId);

        CustomApiResponse<Void> response = new CustomApiResponse<>(
                true,
                "Menu item deleted successfully.",
                HttpStatus.NO_CONTENT.value(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}