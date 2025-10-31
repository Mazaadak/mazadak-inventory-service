package com.mazadak.inventory_service.dto.event;

import java.util.UUID;

public record InventoryDeletedEvent(UUID productId) {
}
