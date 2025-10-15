package com.mazadak.inventory_service.service.Impl;

import com.mazadak.inventory_service.dto.request.AddInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.exception.InventoryNotFoundException;
import com.mazadak.inventory_service.mapper.InventoryMapper;
import com.mazadak.inventory_service.model.Inventory;
import com.mazadak.inventory_service.repository.InventoryRepository;
import com.mazadak.inventory_service.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;


    public Inventory findOrCreateInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createNewInventory(productId));
    }
    Inventory createNewInventory(Long productId) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setTotalQuantity(0);
        inventory.setReservedQuantity(0);
        return inventory;
    }

    @Override
    @Transactional
    public InventoryDTO addInventory(AddInventoryRequest request) {
        Optional<Inventory> existing = inventoryRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return inventoryMapper.toInventoryDTO(existing.get());
        }

        Inventory inventory = findOrCreateInventory(request.productId());

        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.quantity());

        inventory.setIdempotencyKey(request.idempotencyKey());

        return inventoryMapper.toInventoryDTO(inventoryRepository.save(inventory));
    }


    @Override
    public InventoryDTO getInventory(Long productId) {
        return inventoryMapper.toInventoryDTO(findOrCreateInventory(productId));
    }

    @Override
    public InventoryDTO reduceQuantity(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId).
                orElseThrow(()-> new InventoryNotFoundException(productId));
        inventory.reduceQuantity(quantity);
        inventoryRepository.save(inventory);
        return inventoryMapper.toInventoryDTO(inventory);
    }

    @Override
    public void deleteInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId).
                orElseThrow(()-> new InventoryNotFoundException(productId));
        inventoryRepository.delete(inventory);
    }
}
