package com.mazadak.inventory_service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazadak.inventory_service.dto.event.InventoryDeletedEvent;
import com.mazadak.inventory_service.dto.request.AddInventoryRequest;
import com.mazadak.inventory_service.dto.request.UpdateInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.exception.InventoryNotFoundException;
import com.mazadak.inventory_service.exception.NotEnoughInventoryException;
import com.mazadak.inventory_service.mapper.InventoryMapper;
import com.mazadak.inventory_service.model.Inventory;
import com.mazadak.inventory_service.model.OutboxEvent;
import com.mazadak.inventory_service.repository.InventoryRepository;
import com.mazadak.inventory_service.repository.OutboxEventRepository;
import com.mazadak.inventory_service.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public Inventory findInventoryByProductId(UUID productId) {
        log.info("Finding inventory for product {}", productId);
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

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
                inventory.getReservations().clear();
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
        return inventoryMapper.toInventoryDTO(findInventoryByProductId(productId));
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
    @Transactional
    public void deleteInventory(UUID productId) {
        log.info("Deleting inventory for product {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId).
                orElseThrow(()-> {
                    log.error("Inventory Not Found");
                   return new InventoryNotFoundException(productId);
                });
        inventory.setDeleted(true);
        try {
            var deletedEvent = new InventoryDeletedEvent(productId);
            var outboxEvent = new OutboxEvent(
                    "Inventory",
                    "InventoryDeleted",
                    objectMapper.writeValueAsString(deletedEvent)
            );
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize InventoryDeletedEvent for inventory with product {}", productId, e);
        }

        inventoryRepository.save(inventory);
    }

    @Override
    public Boolean existsByProductId(UUID productId) {
        log.info("Checking if inventory exists for product {}", productId);
        return inventoryRepository.existsByProductIdAndDeletedFalse(productId);
    }

    @Override
    public void restoreInventory(UUID productId) {
        var inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        inventory.setDeleted(false);
        inventoryRepository.save(inventory);
    }

    @Override
    public InventoryDTO updateInventory(UUID productId, UpdateInventoryRequest request) {
        int quantity = request.quantity();
        log.info("Updating inventory for product {} to {} ", productId, quantity);
        Inventory inventory = inventoryRepository.findByProductId(productId).
                orElseThrow(()-> {
                    log.error("Inventory Not Found");
                    return new InventoryNotFoundException(productId);
                });
        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (quantity < inventory.getReservedQuantity()) {
            log.error("Not enough inventory");
            throw new NotEnoughInventoryException(
                    inventory.getProductId(),
                    quantity,
                    availableQuantity
            );
        }
        log.info("Updating total quantity to {}", quantity);
        inventory.setTotalQuantity(quantity);
        inventoryRepository.save(inventory);
        log.info("Inventory updated for product {} to {}", productId, quantity);
        return inventoryMapper.toInventoryDTO(inventory);
    }
}
