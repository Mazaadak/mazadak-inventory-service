package com.mazadak.inventory_service.dto.response;


import java.util.UUID;

public record InventoryDTO(
        UUID productId,
        int totalQuantity,
        int reservedQuantity) {
}
