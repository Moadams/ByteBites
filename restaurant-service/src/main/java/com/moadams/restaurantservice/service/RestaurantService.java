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
import com.moadams.restaurantservice.dto.MenuItemRequest;
import com.moadams.restaurantservice.dto.MenuItemResponse;
import com.moadams.restaurantservice.model.MenuItem;
import com.moadams.restaurantservice.repository.MenuItemRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
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

    @Transactional
    public MenuItemResponse createMenuItem(Long restaurantId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        checkRestaurantOwnership(restaurant);

        MenuItem menuItem = MenuItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .available(request.available())
                .restaurant(restaurant)
                .build();

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return mapToMenuItemResponse(savedMenuItem);
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItemsByRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        return menuItemRepository.findByRestaurantId(restaurant.getId()).stream()
                .map(this::mapToMenuItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long restaurantId, Long menuItemId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", menuItemId));

        if (!menuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException(
                    String.format("Menu item with id '%s' not found for restaurant with id '%s'", menuItemId, restaurantId));
        }

        return mapToMenuItemResponse(menuItem);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long restaurantId, Long menuItemId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        checkRestaurantOwnership(restaurant);

        MenuItem existingMenuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", menuItemId));

        if (!existingMenuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new UnauthorizedAccessException(
                    String.format("Menu item with id '%s' does not belong to restaurant with id '%s'", menuItemId, restaurantId));
        }

        existingMenuItem.setName(request.name());
        existingMenuItem.setDescription(request.description());
        existingMenuItem.setPrice(request.price());
        existingMenuItem.setAvailable(request.available());

        MenuItem updatedMenuItem = menuItemRepository.save(existingMenuItem);
        return mapToMenuItemResponse(updatedMenuItem);
    }

    @Transactional
    public void deleteMenuItem(Long restaurantId, Long menuItemId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        checkRestaurantOwnership(restaurant);

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", menuItemId));

        if (!menuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new UnauthorizedAccessException(
                    String.format("Menu item with id '%s' does not belong to restaurant with id '%s'", menuItemId, restaurantId));
        }

        menuItemRepository.delete(menuItem);
    }

    private MenuItemResponse mapToMenuItemResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getAvailable(),
                menuItem.getRestaurant().getId()
        );
    }
}