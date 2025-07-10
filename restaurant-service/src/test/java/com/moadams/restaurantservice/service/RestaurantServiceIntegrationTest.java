package com.moadams.restaurantservice.service;

import com.moadams.restaurantservice.dto.MenuItemRequest;
import com.moadams.restaurantservice.dto.MenuItemResponse;
import com.moadams.restaurantservice.dto.RestaurantRequest;
import com.moadams.restaurantservice.dto.RestaurantResponse;
import com.moadams.restaurantservice.repository.MenuItemRepository;
import com.moadams.restaurantservice.repository.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.kafka.consumer.group-id=test-group",
        "spring.kafka.consumer.bootstrap-servers=localhost:9092",
        "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
        "spring.kafka.producer.bootstrap-servers=localhost:9092",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
class RestaurantServiceIntegrationTest {

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    private String baseUrl;

    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final String TEST_USER_EMAIL = "user@example.com";
    private static final String UNAUTHORIZED_EMAIL = "unauthorized@example.com";

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtDecoder jwtDecoder() {
            return token -> {
                String userEmail;
                if (token.equals("admin_token")) {
                    userEmail = TEST_ADMIN_EMAIL;
                } else if (token.equals("user_token")) {
                    userEmail = TEST_USER_EMAIL;
                } else if (token.equals("unauthorized_token")) {
                    userEmail = UNAUTHORIZED_EMAIL;
                } else {
                    userEmail = "default@example.com";
                }

                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", userEmail);
                claims.put("iss", "http://localhost/mock-auth-server");
                claims.put("aud", "restaurant-service");

                return new Jwt(token, Instant.now(), Instant.now().plusSeconds(3600),
                        Map.of("alg", "none"), claims);
            };
        }
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/restaurants";
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    private HttpHeaders createAuthHeaders(String userType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = "";
        switch (userType) {
            case "admin":
                token = "admin_token";
                break;
            case "user":
                token = "user_token";
                break;
            case "unauthorized":
                token = "unauthorized_token";
                break;
            default:
                token = "guest_token";
                break;
        }
        headers.setBearerAuth(token);
        return headers;
    }

    // ===================== Restaurant API Tests =====================


    @Test
    void createRestaurant_Unauthorized_Returns403() {
        RestaurantRequest request = new RestaurantRequest("Unauthorized Spot", "456 Side Ave", "444-555-6666");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(request, createAuthHeaders("unauthorized"));

        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("You do not have permission");
    }

    @Test
    void createRestaurant_InvalidRequest_Returns400() {
        RestaurantRequest request = new RestaurantRequest("Invalid", "", "111-222-3333");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(request, createAuthHeaders("admin"));

        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Address cannot be blank");
    }

    @Test
    void getRestaurantById_Success() {
        RestaurantRequest createRequest = new RestaurantRequest("Test Diner", "789 Oak Ave", "777-888-9999");
        ResponseEntity<RestaurantResponse> createdResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdResponse.getBody().id());

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("user"));
        ResponseEntity<RestaurantResponse> getResponse = restTemplate.exchange(
                baseUrl + "/" + restaurantId, HttpMethod.GET, entity, RestaurantResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().id()).isEqualTo(restaurantId);
        assertThat(getResponse.getBody().name()).isEqualTo("Test Diner");
    }

    @Test
    void getRestaurantById_NotFound_Returns404() {
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("user"));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/nonExistentId", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Restaurant not found");
    }

    @Test
    void updateRestaurant_Success_ByOwner() {
        RestaurantRequest createRequest = new RestaurantRequest("Update Me", "Original Address", "123-456-7890");
        ResponseEntity<RestaurantResponse> createdResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdResponse.getBody().id());

        RestaurantRequest updateRequest = new RestaurantRequest("Updated Name", "Updated Address", "987-654-3210");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(updateRequest, createAuthHeaders("admin"));
        ResponseEntity<RestaurantResponse> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId, HttpMethod.PUT, entity, RestaurantResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Updated Name");
        assertThat(response.getBody().address()).isEqualTo("Updated Address");
    }

    @Test
    void updateRestaurant_Unauthorized_Returns403() {
        RestaurantRequest createRequest = new RestaurantRequest("Unauthorized Update", "Original Address", "123-456-7890");
        ResponseEntity<RestaurantResponse> createdResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdResponse.getBody().id());

        RestaurantRequest updateRequest = new RestaurantRequest("Should Fail", "Should Fail", "000-000-0000");
        HttpEntity<RestaurantRequest> entity = new HttpEntity<>(updateRequest, createAuthHeaders("user"));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId, HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("You do not have permission");
    }

    @Test
    void deleteRestaurant_Success_ByOwner() {
        RestaurantRequest createRequest = new RestaurantRequest("Delete Me", "To Be Deleted", "111-111-1111");
        ResponseEntity<RestaurantResponse> createdResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdResponse.getBody().id());

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("admin"));
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId, HttpMethod.DELETE, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.exchange(
                baseUrl + "/" + restaurantId, HttpMethod.GET, new HttpEntity<>(createAuthHeaders("user")), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteRestaurant_Unauthorized_Returns403() {
        RestaurantRequest createRequest = new RestaurantRequest("Dont Delete Me", "Safe Address", "222-222-2222");
        ResponseEntity<RestaurantResponse> createdResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdResponse.getBody().id());

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("user"));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId, HttpMethod.DELETE, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("You do not have permission");
    }

    // ===================== Menu Item API Tests =====================

    @Test
    void createMenuItem_Success_ByRestaurantOwner() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Menu Test Restaurant", "Menu Address", "333-333-3333");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest createMenuItemRequest = new MenuItemRequest("Test Burger", "Delicious burger", BigDecimal.valueOf(12.50), true);
        HttpEntity<MenuItemRequest> entity = new HttpEntity<>(createMenuItemRequest, createAuthHeaders("admin"));
        ResponseEntity<MenuItemResponse> response = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", entity, MenuItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Test Burger");
        assertThat(response.getBody().restaurantId()).isEqualTo(restaurantId);
    }

    @Test
    void createMenuItem_UnauthorizedUser_Returns403() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Auth Check Restaurant", "Auth Address", "444-444-4444");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest createMenuItemRequest = new MenuItemRequest("Unauthorized Burger", "Delicious burger", BigDecimal.valueOf(12.50), true);
        HttpEntity<MenuItemRequest> entity = new HttpEntity<>(createMenuItemRequest, createAuthHeaders("user"));
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("You do not have permission");
    }

    @Test
    void getAllMenuItemsByRestaurant_Success() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Menu Listing", "Menu List Address", "555-555-5555");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest item1 = new MenuItemRequest("Burger", "Desc1", BigDecimal.valueOf(10.00), true);
        MenuItemRequest item2 = new MenuItemRequest("Fries", "Desc2", BigDecimal.valueOf(5.00), true);
        restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(item1, createAuthHeaders("admin")), MenuItemResponse.class);
        restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(item2, createAuthHeaders("admin")), MenuItemResponse.class);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("user"));
        ResponseEntity<MenuItemResponse[]> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items", HttpMethod.GET, entity, MenuItemResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(2);
        assertThat(response.getBody()[0].name()).isEqualTo("Burger");
        assertThat(response.getBody()[1].name()).isEqualTo("Fries");
    }

    @Test
    void getMenuItemById_Success() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Single Menu Item Test", "Single Menu Address", "666-666-6666");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest item = new MenuItemRequest("Single Item Pizza", "Single Item Desc", BigDecimal.valueOf(20.00), true);
        ResponseEntity<MenuItemResponse> createdItemResponse = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(item, createAuthHeaders("admin")), MenuItemResponse.class);
        String menuItemId = String.valueOf(createdItemResponse.getBody().id());

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("user"));
        ResponseEntity<MenuItemResponse> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items/" + menuItemId, HttpMethod.GET, entity, MenuItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(menuItemId);
        assertThat(response.getBody().name()).isEqualTo("Single Item Pizza");
    }

    @Test
    void updateMenuItem_Success_ByOwner() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Update Menu Item Test", "Update Menu Address", "777-777-7777");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest createItemRequest = new MenuItemRequest("Old Item", "Old Desc", BigDecimal.valueOf(10.00), true);
        ResponseEntity<MenuItemResponse> createdItemResponse = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(createItemRequest, createAuthHeaders("admin")), MenuItemResponse.class);
        String menuItemId = String.valueOf(createdItemResponse.getBody().id());

        MenuItemRequest updateItemRequest = new MenuItemRequest("New Item", "New Desc", BigDecimal.valueOf(15.00), false);
        HttpEntity<MenuItemRequest> entity = new HttpEntity<>(updateItemRequest, createAuthHeaders("admin"));
        ResponseEntity<MenuItemResponse> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items/" + menuItemId, HttpMethod.PUT, entity, MenuItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("New Item");
        assertThat(response.getBody().price()).isEqualTo(BigDecimal.valueOf(15.00));
        assertThat(response.getBody().available()).isFalse();
    }

    @Test
    void updateMenuItem_UnauthorizedUser_Returns403() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Unauthorized Menu Update", "Unauthorized Menu Address", "999-999-9999");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest createItemRequest = new MenuItemRequest("Item to be updated by unauthorized", "Desc", BigDecimal.valueOf(10), true);
        ResponseEntity<MenuItemResponse> createdItemResponse = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(createItemRequest, createAuthHeaders("admin")), MenuItemResponse.class);
        String menuItemId = String.valueOf(createdItemResponse.getBody().id());

        MenuItemRequest updateItemRequest = new MenuItemRequest("Should Fail Update", "Should Fail Desc", BigDecimal.valueOf(100), true);
        HttpEntity<MenuItemRequest> entity = new HttpEntity<>(updateItemRequest, createAuthHeaders("user"));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items/" + menuItemId, HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("You do not have permission");
    }

    @Test
    void deleteMenuItem_Success_ByOwner() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Delete Menu Item Test", "Delete Menu Address", "888-888-8888");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest createItemRequest = new MenuItemRequest("Item to Delete", "Desc", BigDecimal.valueOf(10), true);
        ResponseEntity<MenuItemResponse> createdItemResponse = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(createItemRequest, createAuthHeaders("admin")), MenuItemResponse.class);
        String menuItemId = String.valueOf(createdItemResponse.getBody().id());

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("admin"));
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items/" + menuItemId, HttpMethod.DELETE, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items/" + menuItemId, HttpMethod.GET, new HttpEntity<>(createAuthHeaders("user")), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteMenuItem_UnauthorizedUser_Returns403() {
        RestaurantRequest createRestaurantRequest = new RestaurantRequest("Unauthorized Menu Delete", "Unauthorized Menu Address", "999-999-9999");
        ResponseEntity<RestaurantResponse> createdRestaurantResponse = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(createRestaurantRequest, createAuthHeaders("admin")), RestaurantResponse.class);
        String restaurantId = String.valueOf(createdRestaurantResponse.getBody().id());

        MenuItemRequest createItemRequest = new MenuItemRequest("Item to be deleted by unauthorized", "Desc", BigDecimal.valueOf(10), true);
        ResponseEntity<MenuItemResponse> createdItemResponse = restTemplate.postForEntity(
                baseUrl + "/" + restaurantId + "/menu-items", new HttpEntity<>(createItemRequest, createAuthHeaders("admin")), MenuItemResponse.class);
        String menuItemId = String.valueOf(createdItemResponse.getBody().id());

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders("user"));
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + restaurantId + "/menu-items/" + menuItemId, HttpMethod.DELETE, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("You do not have permission");
    }
}