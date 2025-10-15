package com.mazadak.inventory_service.controller;


import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.service.InventoryReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventories/reservations")
@RequiredArgsConstructor
public class InventoryReservationController {
    private final InventoryReservationService inventoryReservationService;

    @PostMapping
    public InventoryReservationDTO reserveInventory(
            @Valid @RequestBody ReserveInventoryRequest request) {
        return inventoryReservationService.reserveInventory(request);
    }

    @GetMapping("/{reservationId}")
    public InventoryReservationDTO getReservation(
            @NotNull @PathVariable Long reservationId) {
        return inventoryReservationService.getReservation(reservationId);
    }

    @PutMapping("/{reservationId}/confirm")
    public InventoryReservationDTO confirmReservation(
            @NotNull @PathVariable Long reservationId,
            @Valid @RequestBody ConfirmReservationRequest request) {
        return inventoryReservationService.confirmReservation(reservationId, request);
    }

    @PutMapping("/{reservationId}/release")
    public InventoryReservationDTO releaseReservation(
            @NotNull @PathVariable Long reservationId) {
        return inventoryReservationService.releaseReservation(reservationId);
    }

}
