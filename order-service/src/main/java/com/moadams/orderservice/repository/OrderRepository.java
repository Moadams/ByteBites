package com.moadams.orderservice.repository;

import com.moadams.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserEmail(String userEmail);
    List<Order> findByRestaurantId(String restaurantId);
}