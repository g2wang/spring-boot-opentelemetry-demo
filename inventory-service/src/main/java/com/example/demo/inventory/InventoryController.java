package com.example.demo.inventory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class InventoryController {

    private final Counter lookupCounter;
    private final Map<String, InventoryItem> inventory = Map.of(
            "sku-1001", new InventoryItem("sku-1001", "Trail backpack", 42),
            "sku-1002", new InventoryItem("sku-1002", "Insulated bottle", 18),
            "sku-1003", new InventoryItem("sku-1003", "Camp mug", 0)
    );

    InventoryController(MeterRegistry meterRegistry) {
        this.lookupCounter = Counter.builder("demo.inventory.lookups")
                .description("Number of inventory lookup requests")
                .register(meterRegistry);
    }

    @GetMapping("/api/inventory/{sku}")
    InventoryItem inventory(@PathVariable String sku) {
        lookupCounter.increment();
        return inventory.getOrDefault(sku, new InventoryItem(sku, "Unknown item", 0));
    }

    record InventoryItem(String sku, String name, int quantityAvailable) {
    }
}
