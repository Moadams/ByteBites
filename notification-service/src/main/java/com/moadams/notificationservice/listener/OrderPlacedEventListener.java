package com.moadams.notificationservice.listener;

import com.moadams.notificationservice.event.OrderItemDetails;
import com.moadams.notificationservice.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderPlacedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedEventListener.class);

    @KafkaListener(topics = "order-events-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void listenOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for Order ID: {}", event.orderId());
        log.info("Order Details: Restaurant '{}' for user '{}'", event.restaurantName(), event.userEmail());
        log.info("Total Amount: {}, Delivery Address: {}", event.totalAmount(), event.deliveryAddress());
        log.info("Items: {}", event.orderItems());


        sendNotification(event);
    }

    private void sendNotification(OrderPlacedEvent event) {

        String subject = "Your ByteBites Order #" + event.orderId() + " has been placed!";
        String body = String.format(
                "Dear %s,\n\n" +
                        "Your order from %s has been successfully placed.\n" +
                        "Order ID: %s\n" +
                        "Total Amount: %.2f\n" +
                        "Delivery Address: %s\n\n" +
                        "We will notify you when your order is out for delivery.\n" +
                        "Thank you for choosing ByteBites!\n\n" +
                        "Your Order Items:\n%s",
                event.userEmail(), // Or retrieve user's name if available
                event.restaurantName(),
                event.orderId(),
                event.totalAmount(),
                event.deliveryAddress(),
                formatOrderItems(event.orderItems())
        );

        log.info("--- Simulating Notification to: {} ---", event.userEmail());
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", body);
        log.info("--- End Notification ---");

        // Example: Call a notification service client here
        // notificationServiceClient.sendEmail(event.userEmail(), subject, body);
    }

    private String formatOrderItems(List<OrderItemDetails> items) {
        StringBuilder sb = new StringBuilder();
        for (OrderItemDetails item : items) {
            sb.append(String.format("- %s x %d (%.2f)\n", item.menuItemName(), item.quantity(), item.price()));
        }
        return sb.toString();
    }
}