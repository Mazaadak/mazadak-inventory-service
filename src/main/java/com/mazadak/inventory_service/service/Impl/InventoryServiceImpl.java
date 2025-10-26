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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;


    public Inventory findOrCreateInventory(UUID productId) {
        log.info("Finding or creating inventory for product {}", productId);
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createNewInventory(productId));
    }

    Inventory createNewInventory(UUID productId) {
        log.info("Creating new inventory for product {}", productId);
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setTotalQuantity(0);
        inventory.setReservedQuantity(0);
        return inventory;
    }

    @Override
    @Transactional
    public InventoryDTO addInventory(UUID idempotencyKey, AddInventoryRequest request) {
        log.info("Adding inventory for product {}", request.productId());
        Optional<Inventory> existing = inventoryRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("request has been processed");
            return inventoryMapper.toInventoryDTO(existing.get());
        }

        Inventory inventory = findOrCreateInventory(request.productId());

            if (inventory.isDeleted()) {
                inventory.setDeleted(false);
                inventory.setTotalQuantity(0);
                inventory.setReservations(List.of());
                inventory.setReservedQuantity(0);
            }

        log.info("Updating total quantity");
        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.quantity());

        log.info("Updating idempotency key");
        inventory.setIdempotencyKey(idempotencyKey);

        log.info("Saving inventory");
        return inventoryMapper.toInventoryDTO(inventoryRepository.save(inventory));
    }


    @Override
    public InventoryDTO getInventory(UUID productId) {
        log.info("Getting inventory for product {}", productId);
        return inventoryMapper.toInventoryDTO(findOrCreateInventory(productId));
    }

    @Override
    public InventoryDTO reduceQuantity(UUID productId, int quantity) {
        log.info("Reducing quantity for product {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId).
                orElseThrow(()-> {
                    log.error("Inventory Not Found");
                    return new InventoryNotFoundException(productId);
                });

        log.info("Reducing quantity");
        inventory.reduceQuantity(quantity);

        log.info("Saving inventory");
        inventoryRepository.save(inventory);
        return inventoryMapper.toInventoryDTO(inventory);
    }

    @Override
    public void deleteInventory(UUID productId) {
        log.info("Deleting inventory for product {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId).
                orElseThrow(()-> {
                    log.error("Inventory Not Found");
                   return new InventoryNotFoundException(productId);
                });
        inventory.setDeleted(true);
        inventoryRepository.save(inventory);
    }

    @Override
    public Boolean existsByProductId(UUID productId) {
        return inventoryRepository.existsByProductId(productId);
    }

    @Override
    public void restoreInventory(UUID productId) {
        var inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        inventory.setDeleted(false);
        inventoryRepository.save(inventory);
    }
}
