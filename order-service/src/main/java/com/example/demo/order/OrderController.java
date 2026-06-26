package com.example.demo.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
class OrderController {

    private final RestClient restClient;
    private final String inventoryBaseUrl;
    private final Counter orderCounter;

    OrderController(
            RestClient restClient,
            @Value("${services.inventory.base-url}") String inventoryBaseUrl,
            MeterRegistry meterRegistry
    ) {
        this.restClient = restClient;
        this.inventoryBaseUrl = inventoryBaseUrl;
        this.orderCounter = Counter.builder("demo.orders.requests")
                .description("Number of order lookup requests")
                .register(meterRegistry);
    }

    @GetMapping("/api/orders/{orderId}")
    OrderSummary order(@PathVariable String orderId) {
        orderCounter.increment();
        String sku = switch (orderId) {
            case "1002" -> "sku-1002";
            case "1003" -> "sku-1003";
            default -> "sku-1001";
        };

        InventoryItem inventory = restClient.get()
                .uri(inventoryBaseUrl + "/api/inventory/{sku}", sku)
                .retrieve()
                .body(InventoryItem.class);

        return new OrderSummary(orderId, "user-42", sku, inventory, inventory.quantityAvailable() > 0);
    }

    record OrderSummary(String orderId, String customerId, String sku, InventoryItem inventory, boolean canShip) {
    }

    record InventoryItem(String sku, String name, int quantityAvailable) {
    }
}
