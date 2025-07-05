# ByteBites Microservices Platform

Welcome to the ByteBites Microservices Platform! This project demonstrates a decoupled architecture for a food ordering system using Spring Boot, Spring Cloud, Kafka for asynchronous communication, and Resilience4j for fault tolerance.

## 🚀 Setup Instructions

Follow these steps to get the entire ByteBites platform up and running on your local machine.

### Prerequisites

* **Java 17 or higher:** Ensure `JAVA_HOME` is set.
* **Apache Maven 3.8.x or higher:** For building the projects.
* **Docker Desktop:** Essential for running Kafka and Zookeeper containers.
* **Postman (Optional but Recommended):** For API testing.

### 1. Clone the Repository

```bash
git clone https://github.com/Moadams/ByteBites
cd ByteBites-Platform
```

For the configuration files
```bash
git clone https://github.com/Moadams/ByteBitesConfig
```


### 2. Start Kafka and Zookeeper (using Docker Compose)

Navigate to the root of your project where your docker-compose.yml file is located (assuming it contains Kafka and Zookeeper services).

```bash
docker-compose up -d
```


### 3. Build All Microservices

From the root directory of your project (where the pom.xml file for the parent project resides):

```bash
mvn clean install
```

This command will compile, test, and package all your microservices into .jar files.

### 4. Start Microservices in Order

It's crucial to start the services in a specific order to ensure proper registration and dependency resolution.

#### Service Registry (Eureka Server):

```bash
cd service-registry
mvn spring-boot:run
```

Wait until you see "Started EurekaServerApplication" in the logs.

#### API Gateway:

```bash
cd api-gateway
mvn spring-boot:run
```

Wait until you see "Started GatewayApplication" in the logs.

#### Auth Service:

```bash
cd auth-service
mvn spring-boot:run
```

#### Restaurant Service:

```bash
cd restaurant-service
mvn spring-boot:run
```

#### Order Service:

```bash
cd order-service
mvn spring-boot:run
```

## 🧪 How to Test Each Flow

You can test the APIs using tools like Postman or curl. All API calls should ideally go through the API Gateway.

**Base URL for API Gateway:** `http://localhost:8080`

### 1. User Authentication Flow (User Service)

#### Register a User:

- **Method:** POST
- **URL:** `http://localhost:8080/api/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body (JSON):**

```json
{
    "email": "test@example.com",
    "password": "password123"
}
```

#### Login User & Get JWT:

- **Method:** POST
- **URL:** `http://localhost:8080/api/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body (JSON):**

```json
{
    "email": "test@example.com",
    "password": "password123"
}
```

**Response:** You will receive a JWT (access_token) in the response body. Copy this token for subsequent requests.

### 2. Restaurant Data Flow (Restaurant Service)

#### Get All Restaurants:

- **Method:** GET
- **URL:** `http://localhost:8080/api/restaurants`
- **Headers:** `Authorization: Bearer <YOUR_JWT_TOKEN>`

#### Get Restaurant by ID:

- **Method:** GET
- **URL:** `http://localhost:8080/api/restaurants/{restaurantId}` (e.g., `http://localhost:8080/api/restaurants/some-restaurant-id`)
- **Headers:** `Authorization: Bearer <YOUR_JWT_TOKEN>`

#### Get Menu Item by ID (from a specific restaurant):

- **Method:** GET
- **URL:** `http://localhost:8080/api/restaurants/{restaurantId}/menu-items/{menuItemId}`
- **Headers:** `Authorization: Bearer <YOUR_JWT_TOKEN>`

### 3. Order Management Flow (Order Service)

#### Create a New Order:

- **Method:** POST
- **URL:** `http://localhost:8080/api/orders`
- **Headers:** `Content-Type: application/json`, `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Body (JSON):**

```json
{
    "restaurantId": "restaurant-id-from-restaurant-service",
    "deliveryAddress": "123 Main St, Anytown",
    "orderItems": [
        {
            "menuItemId": "menu-item-id-from-restaurant",
            "menuItemName": "Burger",
            "quantity": 1,
            "price": 12.50
        },
        {
            "menuItemId": "another-menu-item-id",
            "menuItemName": "Fries",
            "quantity": 1,
            "price": 3.00
        }
    ]
}
```

**Note:** The order-service will attempt to fetch restaurant and menu item details from the restaurant-service to validate and calculate total amount. This is where the Circuit Breaker comes into play.

#### Get Order by ID:

- **Method:** GET
- **URL:** `http://localhost:8080/api/orders/{orderId}`
- **Headers:** `Authorization: Bearer <YOUR_JWT_TOKEN>`

#### Get Orders by User Email:

- **Method:** GET
- **URL:** `http://localhost:8080/api/orders/user/{userEmail}` (e.g., `http://localhost:8080/api/orders/user/test@example.com`)
- **Headers:** `Authorization: Bearer <YOUR_JWT_TOKEN>`

## 🔒 JWT Testing with Postman

1. **Perform a Login Request:** Use the "Login User & Get JWT" instructions above.

2. **Copy the access_token:** From the JSON response, copy the entire string value of the access_token field.

3. **Set Authorization Header:** For all subsequent requests to protected endpoints (most of your microservice endpoints):
    - Go to the "Headers" tab in Postman.
    - Add a new header:
        - **Key:** `Authorization`
        - **Value:** `Bearer <PASTE_YOUR_JWT_TOKEN_HERE>` (Make sure there's a space after "Bearer").


## 🗺️ Architecture Diagram

This diagram illustrates the core components of the ByteBites Microservices Platform, showing how requests flow and how services interact, specifically highlighting JWT authentication and Kafka messaging.

```mermaid
graph TD
    subgraph Client Interaction
        A[Client/Postman]
    end

    subgraph Infrastructure
        K[Apache Kafka]
        E[Eureka Server<br>(Service Registry)]
    end

    subgraph Gateway
        G[API Gateway<br>(Spring Cloud Gateway)]
    end

    subgraph Core Microservices
        US[User Service<br>(Auth & User Mgmt)]
        RS[Restaurant Service<br>(Restaurant & Menu Mgmt)]
        OS[Order Service<br>(Order Mgmt)]
    end

    %% Flow for JWT Authentication
    A -- 1. User Register/Login Request --> G
    G -- 2. Route to User Service --> US
    US -- 3. Authenticate & Generate JWT --> A{JWT Token}

    %% Flow for subsequent API calls with JWT
    A -- 4. API Request (with JWT) --> G
    G -- 5. Validate JWT & Discover Service --> RS
    G -- 5. Validate JWT & Discover Service --> OS
    G -- 5. Validate JWT & Discover Service --> US (e.g., Get User Profile)

    %% Service Registration with Eureka
    US -- Registers Self --> E
    RS -- Registers Self --> E
    OS -- Registers Self --> E
    G -- Discovers Services --> E

    %% Inter-service communication via REST (Sync)
    OS -- 6. Validate Restaurant/Menu Items<br>(Resilience4j Circuit Breaker) --> RS

    %% Asynchronous Communication via Kafka
    OS -- 7. Order Placed Event --> K
    K -- 8. Event Consumed --> RS (e.g., to update restaurant's order list/status)
    K -- 8. Event Consumed --> PS(Notification Service)
```

## 📋 Service Ports

| Service | Port |
|---------|------|
| Eureka Server | 8761 |
| API Gateway | 8080 |
| User Service | 8081 |
| Restaurant Service | 8082 |
| Order Service | 8083 |

## 🛠️ Technologies Used

- **Spring Boot** - Microservices framework
- **Spring Cloud Gateway** - API Gateway
- **Spring Cloud Netflix Eureka** - Service Discovery
- **Spring Security** - Authentication and Authorization
- **JWT** - Token-based authentication
- **Apache Kafka** - Asynchronous messaging
- **Resilience4j** - Circuit Breaker pattern
- **Spring Data JPA** - Data persistence
- **Docker** - Containerization
- **Maven** - Build tool