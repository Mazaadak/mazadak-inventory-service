package com.mazadak.inventory_service.controller;


import com.mazadak.inventory_service.dto.request.AddInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryDTO> addInventory(
            @RequestHeader("Idempotency-Key") @NotNull UUID idempotencyKey,
           @Valid @RequestBody AddInventoryRequest request) {

        return ResponseEntity.ok(inventoryService.addInventory(idempotencyKey, request));
    }

    @PatchMapping("/{productId}/reduce")
    public ResponseEntity<InventoryDTO> reduceInventory(
            @PathVariable @NotNull UUID productId ,
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {

        return ResponseEntity.ok(inventoryService.reduceQuantity(productId, quantity));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryDTO> getInventoryByProductId(
            @PathVariable @NotNull UUID productId) {

        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteInventory(
            @PathVariable @NotNull UUID productId) {

        inventoryService.deleteInventory(productId);
        return ResponseEntity.noContent().build();
    }

}
