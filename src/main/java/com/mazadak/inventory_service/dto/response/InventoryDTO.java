package com.mazadak.inventory_service.dto.response;



public record InventoryDTO(
        Long productId,
        int totalQuantity,
        int reservedQuantity) {
}
