package com.example.demo.storefront;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@RestController
class StorefrontController {

    private final RestClient restClient;
    private final String orderBaseUrl;
    private final Counter pageCounter;

    StorefrontController(
            RestClient restClient,
            @Value("${services.order.base-url}") String orderBaseUrl,
            MeterRegistry meterRegistry
    ) {
        this.restClient = restClient;
        this.orderBaseUrl = orderBaseUrl;
        this.pageCounter = Counter.builder("demo.storefront.order_pages")
                .description("Number of order page requests")
                .register(meterRegistry);
    }

    @GetMapping("/")
    String home() {
        return "Spring Boot OpenTelemetry demo. Try /api/storefront/orders/1001";
    }

    @GetMapping("/api/storefront/orders/{orderId}")
    StorefrontOrderPage orderPage(@PathVariable String orderId) {
        pageCounter.increment();
        OrderSummary order = restClient.get()
                .uri(orderBaseUrl + "/api/orders/{orderId}", orderId)
                .retrieve()
                .body(OrderSummary.class);

        String message = order.canShip() ? "Ready to ship" : "Waiting for inventory";
        return new StorefrontOrderPage(order, message, Instant.now());
    }

    record StorefrontOrderPage(OrderSummary order, String message, Instant renderedAt) {
    }

    record OrderSummary(String orderId, String customerId, String sku, InventoryItem inventory, boolean canShip) {
    }

    record InventoryItem(String sku, String name, int quantityAvailable) {
    }
}
