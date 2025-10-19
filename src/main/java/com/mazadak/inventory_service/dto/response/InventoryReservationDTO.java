package com.mazadak.inventory_service.dto.response;

import com.mazadak.inventory_service.model.enums.ReservationStatus;

import java.util.UUID;

public record InventoryReservationDTO (
        UUID inventoryReservationId,
        UUID productId,
        int quantity,
        ReservationStatus status){
}
