package com.mazadak.inventory_service.service;

import com.mazadak.inventory_service.dto.request.AddInventoryRequest;
import com.mazadak.inventory_service.dto.request.UpdateInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.model.Inventory;

import java.util.UUID;

public interface InventoryService {

    Inventory findOrCreateInventory(UUID productId);

    InventoryDTO getInventory(UUID productId);

    InventoryDTO addInventory(UUID idempotencyKey, AddInventoryRequest request);

    InventoryDTO reduceQuantity(UUID productId, int quantity);

    InventoryDTO updateInventory(UUID productId, UpdateInventoryRequest request);

    void deleteInventory(UUID productId);

}
