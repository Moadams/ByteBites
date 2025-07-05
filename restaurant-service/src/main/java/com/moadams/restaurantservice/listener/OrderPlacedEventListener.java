package com.moadams.restaurantservice.listener;

import com.moadams.restaurantservice.event.OrderItemDetails;
import com.moadams.restaurantservice.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedEventListener.class);

    @KafkaListener(topics = "order-events-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void listenOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Restaurant Service received OrderPlacedEvent for Order ID: {}", event.orderId());
        log.info("Order placed for Restaurant: {} ({})", event.restaurantName(), event.restaurantId());
        log.info("Delivery Address: {}", event.deliveryAddress());

        log.info("--- Starting preparation for Order #{} ---", event.orderId());
        for (OrderItemDetails item : event.orderItems()) {
            log.info("  - Preparing: {} (x{})", item.menuItemName(), item.quantity());
        }
        log.info("--- Order preparation started for Order #{} ---", event.orderId());
    }
}