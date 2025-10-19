package com.mazadak.inventory_service.service;

import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.model.InventoryReservation;

import java.util.List;
import java.util.UUID;

public interface InventoryReservationService {

    List<UUID> reserveInventory(UUID idempotencyKey, ReserveInventoryRequest request);

    List<InventoryReservationDTO> releaseReservation(UUID idempotencyKey, List<UUID> reservationIds);

    List<InventoryReservationDTO> confirmReservation(UUID idempotencyKey, ConfirmReservationRequest request);

    InventoryReservationDTO getReservation(UUID reservationId);

}
