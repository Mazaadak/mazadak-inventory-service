package com.mazadak.inventory_service.service;

import com.mazadak.inventory_service.dto.request.AddInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.model.Inventory;

public interface InventoryService {

    Inventory findOrCreateInventory(Long productId);

    InventoryDTO getInventory(Long productId);

    InventoryDTO addInventory(AddInventoryRequest request);

    InventoryDTO reduceQuantity(Long productId, int quantity);

    void deleteInventory(Long productId);

}
