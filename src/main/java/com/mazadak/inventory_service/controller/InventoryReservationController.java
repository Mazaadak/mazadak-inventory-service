package com.mazadak.inventory_service.controller;


import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.service.InventoryReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventories/reservations")
@RequiredArgsConstructor
public class InventoryReservationController {
    private final InventoryReservationService inventoryReservationService;

    @PostMapping
    public ResponseEntity<List<UUID> > reserveInventory(
            @RequestHeader("Idempotency-Key") @NotNull UUID idempotencyKey,
            @Valid @RequestBody ReserveInventoryRequest request) {
        return ResponseEntity.ok(inventoryReservationService.reserveInventory(idempotencyKey, request));
    }

    @GetMapping("/{reservationId}")
    public InventoryReservationDTO getReservation(
            @NotNull @PathVariable UUID reservationId) {
        return inventoryReservationService.getReservation(reservationId);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmReservation(
            @RequestHeader("Idempotency-Key") @NotNull UUID idempotencyKey,
            @Valid @RequestBody ConfirmReservationRequest request) {
         inventoryReservationService.confirmReservation(idempotencyKey, request);
         return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseReservation(
            @RequestHeader("Idempotency-Key") @NotNull UUID idempotencyKey,
            @NotNull @RequestBody List<UUID> reservationIds) {
         inventoryReservationService.releaseReservation(idempotencyKey, reservationIds);
         return ResponseEntity.ok().build();
    }

}
