package com.mazadak.inventory_service.dto.response;

import com.mazadak.inventory_service.model.enums.ReservationStatus;

public record InventoryReservationDTO (
        Long inventoryReservationId,
        Long productId,
        int quantity,
        ReservationStatus status){
}
