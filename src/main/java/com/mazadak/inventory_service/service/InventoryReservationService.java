package com.mazadak.inventory_service.service;

import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.model.InventoryReservation;

public interface InventoryReservationService {

    InventoryReservationDTO reserveInventory(ReserveInventoryRequest request);

    InventoryReservationDTO releaseReservation(Long reservationId);

    InventoryReservationDTO confirmReservation(Long reservationId, ConfirmReservationRequest request);

    InventoryReservationDTO getReservation(Long reservationId);

}
