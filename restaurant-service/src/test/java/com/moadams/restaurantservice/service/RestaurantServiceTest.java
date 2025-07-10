package com.moadams.restaurantservice.service;

import com.moadams.restaurantservice.dto.MenuItemRequest;
import com.moadams.restaurantservice.dto.MenuItemResponse;
import com.moadams.restaurantservice.dto.RestaurantRequest;
import com.moadams.restaurantservice.dto.RestaurantResponse;
import com.moadams.restaurantservice.exception.ResourceNotFoundException;
import com.moadams.restaurantservice.exception.UnauthorizedAccessException;
import com.moadams.restaurantservice.model.MenuItem;
import com.moadams.restaurantservice.model.Restaurant;
import com.moadams.restaurantservice.repository.MenuItemRepository;
import com.moadams.restaurantservice.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RestaurantService restaurantService;

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String OTHER_USER_EMAIL = "other@example.com";


    private void setupAuthenticatedUser(String email) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(email);
    }


    private void setupUnauthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
    }

    // ===================== Restaurant Tests =====================

    @Test
    void createRestaurant_Success() {
        RestaurantRequest restaurantRequest = new RestaurantRequest("Pizza Palace", "Ahodwo Las Vegas", "0544999990");
        Restaurant savedRestaurant = createTestRestaurant(1L, TEST_USER_EMAIL);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);

            RestaurantResponse restaurantResponse = restaurantService.createRestaurant(restaurantRequest);

            assertNotNull(restaurantResponse);
            assertEquals(1L, restaurantResponse.id());
            assertEquals("Pizza Palace", restaurantResponse.name());
            assertEquals("Ahodwo Las Vegas", restaurantResponse.address());
            assertEquals("0544999990", restaurantResponse.phone());
            assertEquals(TEST_USER_EMAIL, restaurantResponse.ownerEmail());
            verify(restaurantRepository).save(any(Restaurant.class));
        }
    }

    @Test
    void createRestaurant_UnauthenticatedUser_ThrowsException() {
        RestaurantRequest restaurantRequest = new RestaurantRequest("Pizza Palace", "Ahodwo Las Vegas", "0544999990");

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupUnauthenticatedUser();

            assertThrows(UnauthorizedAccessException.class, () ->
                    restaurantService.createRestaurant(restaurantRequest));
            verify(restaurantRepository, never()).save(any(Restaurant.class));
        }
    }

    @Test
    void getAllRestaurants_Success() {
        List<Restaurant> restaurants = Arrays.asList(
                createTestRestaurant(1L, TEST_USER_EMAIL),
                createTestRestaurant(2L, OTHER_USER_EMAIL)
        );
        when(restaurantRepository.findAll()).thenReturn(restaurants);
        List<RestaurantResponse> restaurantResponses = restaurantService.getAllRestaurants();

        assertNotNull(restaurantResponses);
        assertEquals(2, restaurantResponses.size());
        verify(restaurantRepository).findAll();
    }

    @Test
    void getRestaurantById_Success() {

        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse restaurantResponse = restaurantService.getRestaurantById(1L);

        assertNotNull(restaurantResponse);
        assertEquals(1L, restaurantResponse.id());
        assertEquals("Pizza Palace", restaurantResponse.name());
        verify(restaurantRepository).findById(1L);
    }

    @Test
    void getRestaurantById_NotFound_ThrowsException() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                restaurantService.getRestaurantById(1L));

        assertTrue(exception.getMessage().contains("Restaurant"));
        verify(restaurantRepository).findById(1L);
    }

    @Test
    void updateRestaurant_Success() {
        RestaurantRequest request = new RestaurantRequest("Pizzaman Chickenman", "Amakom", "0200001982");
        Restaurant existingRestaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        Restaurant updatedRestaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        updatedRestaurant.setName("Pizzaman Chickenman");
        updatedRestaurant.setAddress("Amakom");
        updatedRestaurant.setPhone("0200001982");

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existingRestaurant));
            when(restaurantRepository.save(any(Restaurant.class))).thenReturn(updatedRestaurant);

            RestaurantResponse response = restaurantService.updateRestaurant(1L, request);

            assertNotNull(response);
            assertEquals("Pizzaman Chickenman", response.name());
            assertEquals("Amakom", response.address());
            assertEquals("0200001982", response.phone());
            verify(restaurantRepository).save(any(Restaurant.class));
        }
    }

    @Test
    void updateRestaurant_UnauthorizedUser_ThrowsException() {
        RestaurantRequest request = new RestaurantRequest("Pizzaman Chickenman", "Amakom", "0200001982");
        Restaurant existingRestaurant = createTestRestaurant(1L, OTHER_USER_EMAIL);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existingRestaurant));

            assertThrows(UnauthorizedAccessException.class, () ->
                    restaurantService.updateRestaurant(1L, request));
            verify(restaurantRepository, never()).save(any(Restaurant.class));
        }
    }

    @Test
    void deleteRestaurant_Success() {

        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

            restaurantService.deleteRestaurant(1L);

            verify(restaurantRepository).delete(restaurant);
        }
    }

    @Test
    void deleteRestaurant_UnauthorizedUser_ThrowsException() {
        Restaurant restaurant = createTestRestaurant(1L, OTHER_USER_EMAIL);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

            assertThrows(UnauthorizedAccessException.class, () ->
                    restaurantService.deleteRestaurant(1L));
            verify(restaurantRepository, never()).delete(any(Restaurant.class));
        }
    }

    // ===================== Menu Item Tests =====================

    @Test
    void createMenuItem_Success() {
        MenuItemRequest request = new MenuItemRequest("Chrisbreezy Pizza", "Classic tomato and mozzarella",
                BigDecimal.valueOf(15.99), true);
        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        MenuItem savedMenuItem = createTestMenuItem(1L, restaurant);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
            when(menuItemRepository.save(any(MenuItem.class))).thenReturn(savedMenuItem);

            MenuItemResponse response = restaurantService.createMenuItem(1L, request);

            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("Chrisbreezy Pizza", response.name());
            assertEquals("Classic tomato and mozzarella", response.description());
            assertEquals(BigDecimal.valueOf(15.99), response.price());
            assertTrue(response.available());
            assertEquals(1L, response.restaurantId());
            verify(menuItemRepository).save(any(MenuItem.class));
        }
    }

    @Test
    void createMenuItem_UnauthorizedUser_ThrowsException() {
        MenuItemRequest request = new MenuItemRequest("Chrisbreezy Pizza", "Classic tomato and mozzarella",
                BigDecimal.valueOf(15.99), true);
        Restaurant restaurant = createTestRestaurant(1L, OTHER_USER_EMAIL);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

            assertThrows(UnauthorizedAccessException.class, () ->
                    restaurantService.createMenuItem(1L, request));
            verify(menuItemRepository, never()).save(any(MenuItem.class));
        }
    }

    @Test
    void getAllMenuItemsByRestaurant_Success() {
        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        List<MenuItem> menuItems = Arrays.asList(
                createTestMenuItem(1L, restaurant),
                createTestMenuItem(2L, restaurant)
        );

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantId(1L)).thenReturn(menuItems);

        List<MenuItemResponse> responses = restaurantService.getAllMenuItemsByRestaurant(1L);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(menuItemRepository).findByRestaurantId(1L);
    }

    @Test
    void getMenuItemById_Success() {
        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        MenuItem menuItem = createTestMenuItem(1L, restaurant);

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

        MenuItemResponse response = restaurantService.getMenuItemById(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Chrisbreezy Pizza", response.name());
        verify(menuItemRepository).findById(1L);
    }

    @Test
    void getMenuItemById_MenuItemNotInRestaurant_ThrowsException() {
        Restaurant restaurant1 = createTestRestaurant(1L, TEST_USER_EMAIL);
        Restaurant restaurant2 = createTestRestaurant(2L, OTHER_USER_EMAIL);
        MenuItem menuItem = createTestMenuItem(1L, restaurant2);

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant1));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

        assertThrows(ResourceNotFoundException.class, () ->
                restaurantService.getMenuItemById(1L, 1L));
    }

    @Test
    void updateMenuItem_Success() {
        MenuItemRequest request = new MenuItemRequest("Updated Pizza", "Updated description",
                BigDecimal.valueOf(18.99), false);
        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        MenuItem existingMenuItem = createTestMenuItem(1L, restaurant);
        MenuItem updatedMenuItem = createTestMenuItem(1L, restaurant);
        updatedMenuItem.setName("Updated Pizza");
        updatedMenuItem.setDescription("Updated description");
        updatedMenuItem.setPrice(BigDecimal.valueOf(18.99));
        updatedMenuItem.setAvailable(false);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
            when(menuItemRepository.findById(1L)).thenReturn(Optional.of(existingMenuItem));
            when(menuItemRepository.save(any(MenuItem.class))).thenReturn(updatedMenuItem);

            MenuItemResponse response = restaurantService.updateMenuItem(1L, 1L, request);

            assertNotNull(response);
            assertEquals("Updated Pizza", response.name());
            assertEquals("Updated description", response.description());
            assertEquals(BigDecimal.valueOf(18.99), response.price());
            assertFalse(response.available());
            verify(menuItemRepository).save(any(MenuItem.class));
        }
    }

    @Test
    void updateMenuItem_MenuItemNotInRestaurant_ThrowsException() {
        MenuItemRequest request = new MenuItemRequest("Updated Pizza", "Updated description",
                BigDecimal.valueOf(18.99), false);
        Restaurant restaurant1 = createTestRestaurant(1L, TEST_USER_EMAIL);
        Restaurant restaurant2 = createTestRestaurant(2L, OTHER_USER_EMAIL);
        MenuItem menuItem = createTestMenuItem(1L, restaurant2);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant1));
            when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

            assertThrows(UnauthorizedAccessException.class, () ->
                    restaurantService.updateMenuItem(1L, 1L, request));
            verify(menuItemRepository, never()).save(any(MenuItem.class));
        }
    }

    @Test
    void deleteMenuItem_Success() {
        Restaurant restaurant = createTestRestaurant(1L, TEST_USER_EMAIL);
        MenuItem menuItem = createTestMenuItem(1L, restaurant);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
            when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

            restaurantService.deleteMenuItem(1L, 1L);

            verify(menuItemRepository).delete(menuItem);
        }
    }

    @Test
    void deleteMenuItem_UnauthorizedUser_ThrowsException() {
        Restaurant restaurant = createTestRestaurant(1L, OTHER_USER_EMAIL);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            setupAuthenticatedUser(TEST_USER_EMAIL);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

            assertThrows(UnauthorizedAccessException.class, () ->
                    restaurantService.deleteMenuItem(1L, 1L));
            verify(menuItemRepository, never()).delete(any(MenuItem.class));
        }
    }

    // ===================== Helper Methods =====================

    private Restaurant createTestRestaurant(Long id, String ownerEmail) {
        return Restaurant.builder()
                .id(id)
                .name("Pizza Palace")
                .address("Ahodwo Las Vegas")
                .phone("0544999990")
                .ownerEmail(ownerEmail)
                .build();
    }

    private MenuItem createTestMenuItem(Long id, Restaurant restaurant) {
        return MenuItem.builder()
                .id(id)
                .name("Chrisbreezy Pizza")
                .description("Classic tomato and mozzarella")
                .price(BigDecimal.valueOf(15.99))
                .available(true)
                .restaurant(restaurant)
                .build();
    }
}