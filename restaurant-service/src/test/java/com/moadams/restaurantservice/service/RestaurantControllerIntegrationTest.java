package com.moadams.restaurantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moadams.restaurantservice.dto.CustomApiResponse;
import com.moadams.restaurantservice.dto.RestaurantRequest;
import com.moadams.restaurantservice.dto.RestaurantResponse;
import com.moadams.restaurantservice.model.Restaurant;
import com.moadams.restaurantservice.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RestaurantControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/restaurants";
        restaurantRepository.deleteAll();
    }

    private HttpHeaders createAuthHeaders(String email, String roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Auth-User-Email", email);
        headers.set("X-Auth-User-Roles", roles);
        return headers;
    }

    @Test
    @Transactional
    void createRestaurant_WithValidData_ShouldReturnCreatedRestaurant() {
        RestaurantRequest request = new RestaurantRequest(
                "Test Restaurant",
                "123 Test Street",
                "+1234567890"
        );

        HttpHeaders headers = createAuthHeaders("owner@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<CustomApiResponse<RestaurantResponse>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<RestaurantResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().name()).isEqualTo("Test Restaurant");
        assertThat(response.getBody().data().ownerEmail()).isEqualTo("owner@test.com");

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertThat(restaurants).hasSize(1);
        assertThat(restaurants.get(0).getName()).isEqualTo("Test Restaurant");
    }

    @Test
    void createRestaurant_WithInvalidData_ShouldReturnBadRequest() {
        RestaurantRequest request = new RestaurantRequest(
                "",
                "123 Test Street",
                "invalid-phone"
        );

        HttpHeaders headers = createAuthHeaders("owner@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation failed");
    }

    @Test
    void createRestaurant_WithoutAuthentication_ShouldReturnForbidden() {
        RestaurantRequest request = new RestaurantRequest(
                "Test Restaurant",
                "123 Test Street",
                "+1234567890"
        );

        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createRestaurant_WithWrongRole_ShouldReturnForbidden() {
        RestaurantRequest request = new RestaurantRequest(
                "Test Restaurant",
                "123 Test Street",
                "+1234567890"
        );

        HttpHeaders headers = createAuthHeaders("customer@test.com", "ROLE_CUSTOMER");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Transactional
    void getAllRestaurants_ShouldReturnAllRestaurants() {
        Restaurant restaurant1 = Restaurant.builder()
                .name("Restaurant 1")
                .address("Address 1")
                .phone("+1111111111")
                .ownerEmail("owner1@test.com")
                .build();

        Restaurant restaurant2 = Restaurant.builder()
                .name("Restaurant 2")
                .address("Address 2")
                .phone("+2222222222")
                .ownerEmail("owner2@test.com")
                .build();

        restaurantRepository.saveAll(List.of(restaurant1, restaurant2));

        HttpHeaders headers = createAuthHeaders("customer@test.com", "ROLE_CUSTOMER");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CustomApiResponse<List<RestaurantResponse>>> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<List<RestaurantResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).hasSize(2);
    }

    @Test
    @Transactional
    void getMyRestaurants_ShouldReturnOnlyOwnedRestaurants() {
        Restaurant ownedRestaurant = Restaurant.builder()
                .name("My Restaurant")
                .address("My Address")
                .phone("+1111111111")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant otherRestaurant = Restaurant.builder()
                .name("Other Restaurant")
                .address("Other Address")
                .phone("+2222222222")
                .ownerEmail("other@test.com")
                .build();

        restaurantRepository.saveAll(List.of(ownedRestaurant, otherRestaurant));

        HttpHeaders headers = createAuthHeaders("owner@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CustomApiResponse<List<RestaurantResponse>>> response = restTemplate.exchange(
                baseUrl + "/my",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<List<RestaurantResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).name()).isEqualTo("My Restaurant");
    }

    @Test
    @Transactional
    void getRestaurantById_WithValidId_ShouldReturnRestaurant() {
        Restaurant restaurant = Restaurant.builder()
                .name("Test Restaurant")
                .address("Test Address")
                .phone("+1234567890")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        HttpHeaders headers = createAuthHeaders("customer@test.com", "ROLE_CUSTOMER");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CustomApiResponse<RestaurantResponse>> response = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<RestaurantResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().name()).isEqualTo("Test Restaurant");
    }

    @Test
    void getRestaurantById_WithInvalidId_ShouldReturnNotFound() {
        HttpHeaders headers = createAuthHeaders("customer@test.com", "ROLE_CUSTOMER");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CustomApiResponse<Void>> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    @Transactional
    void updateRestaurant_AsOwner_ShouldUpdateSuccessfully() {
        Restaurant restaurant = Restaurant.builder()
                .name("Original Name")
                .address("Original Address")
                .phone("+1234567890")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        RestaurantRequest updateRequest = new RestaurantRequest(
                "Updated Name",
                "Updated Address",
                "+0987654321"
        );

        HttpHeaders headers = createAuthHeaders("owner@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<CustomApiResponse<RestaurantResponse>> response = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<RestaurantResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().name()).isEqualTo("Updated Name");

        Restaurant updatedRestaurant = restaurantRepository.findById(savedRestaurant.getId()).orElseThrow();
        assertThat(updatedRestaurant.getName()).isEqualTo("Updated Name");
    }

    @Test
    @Transactional
    void updateRestaurant_AsNonOwner_ShouldReturnForbidden() {
        Restaurant restaurant = Restaurant.builder()
                .name("Original Name")
                .address("Original Address")
                .phone("+1234567890")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        RestaurantRequest updateRequest = new RestaurantRequest(
                "Updated Name",
                "Updated Address",
                "+0987654321"
        );

        HttpHeaders headers = createAuthHeaders("different@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.PUT,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Transactional
    void deleteRestaurant_AsOwner_ShouldDeleteSuccessfully() {
        Restaurant restaurant = Restaurant.builder()
                .name("To Delete")
                .address("Delete Address")
                .phone("+1234567890")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        HttpHeaders headers = createAuthHeaders("owner@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CustomApiResponse<Void>> response = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(restaurantRepository.findById(savedRestaurant.getId())).isEmpty();
    }

    @Test
    @Transactional
    void deleteRestaurant_AsNonOwner_ShouldReturnForbidden() {
        Restaurant restaurant = Restaurant.builder()
                .name("To Delete")
                .address("Delete Address")
                .phone("+1234567890")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        HttpHeaders headers = createAuthHeaders("different@test.com", "ROLE_RESTAURANT_OWNER");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.DELETE,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(restaurantRepository.findById(savedRestaurant.getId())).isPresent();
    }

    @Test
    @Transactional
    void adminCanManageAnyRestaurant_ShouldAllowAllOperations() {
        // Given
        Restaurant restaurant = Restaurant.builder()
                .name("Admin Test")
                .address("Admin Address")
                .phone("+1234567890")
                .ownerEmail("owner@test.com")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        RestaurantRequest updateRequest = new RestaurantRequest(
                "Admin Updated",
                "Admin Updated Address",
                "+0987654321"
        );

        HttpHeaders headers = createAuthHeaders("admin@test.com", "ROLE_ADMIN");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(updateRequest, headers);

        // When - Update as admin
        ResponseEntity<CustomApiResponse<RestaurantResponse>> updateResponse = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<CustomApiResponse<RestaurantResponse>>() {}
        );

        // Then
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().data().name()).isEqualTo("Admin Updated");

        // When - Delete as admin
        HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<CustomApiResponse<Void>> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + savedRestaurant.getId(),
                HttpMethod.DELETE,
                deleteEntity,
                new ParameterizedTypeReference<CustomApiResponse<Void>>() {}
        );

        // Then
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(restaurantRepository.findById(savedRestaurant.getId())).isEmpty();
    }
}
