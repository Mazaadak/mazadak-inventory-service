package com.mazadak.inventory_service.service.Impl;

import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.exception.NotEnoughInventoryException;
import com.mazadak.inventory_service.exception.ReservationNotFoundException;
import com.mazadak.inventory_service.mapper.InventoryReservationMapper;
import com.mazadak.inventory_service.model.Inventory;
import com.mazadak.inventory_service.model.InventoryReservation;
import com.mazadak.inventory_service.model.enums.ReservationStatus;
import com.mazadak.inventory_service.repository.InventoryRepository;
import com.mazadak.inventory_service.repository.InventoryReservationRepository;
import com.mazadak.inventory_service.service.InventoryReservationService;
import com.mazadak.inventory_service.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryService inventoryService;
    private final InventoryReservationMapper inventoryReservationMapper;


    @Override
    @Transactional
    public InventoryReservationDTO reserveInventory(ReserveInventoryRequest request) {
        // Check for existing reservation with the same idempotency key
        Optional<InventoryReservation> inventoryReservation= inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(
                request.productId(), request.idempotencyKey());

        if (inventoryReservation.isPresent()) { // request has been processed
            return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation.get());
        }

        Inventory inventory = inventoryService.findOrCreateInventory(request.productId());

        // Check if there's enough inventory
        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();

        if (availableQuantity < request.quantity()) {
            throw new NotEnoughInventoryException(
                inventory.getProductId(), 
                request.quantity(), 
                availableQuantity
            );
        }

        // Update inventory with idempotency key
        inventory.setIdempotencyKey(request.idempotencyKey());
        
        // Update reserved quantity
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        inventory = inventoryRepository.save(inventory);

        // Create reservation
        InventoryReservation updatedReservation = InventoryReservation.builder()
                .userId(request.userId())
                .inventory(inventory)
                .quantity(request.quantity())
                .status(ReservationStatus.RESERVED)
                .idempotencyKey(request.idempotencyKey())
                .build();

        // Save reservation
        inventoryReservationRepository.save(updatedReservation);

        return inventoryReservationMapper.toInventoryReservationDTO(updatedReservation);
    }

    @Override
    @Transactional
    public InventoryReservationDTO releaseReservation(Long reservationId) {

        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // update reservation status
        inventoryReservation.release();

        // reduce reserved quantity in the inventory
        inventoryReservation.getInventory()
                .setReservedQuantity(inventoryReservation.getInventory()
                        .getReservedQuantity() - inventoryReservation.getQuantity());

        // save changes
        inventoryRepository.save(inventoryReservation.getInventory());
        inventoryReservationRepository.save(inventoryReservation);

        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }

    @Override
    @Transactional
    public InventoryReservationDTO confirmReservation(Long reservationId, ConfirmReservationRequest request) {

        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Update reservation status
        inventoryReservation.confirm(request.orderId());

        //reduce total quantity in the inventory
        inventoryReservation.getInventory()
                        .setTotalQuantity(inventoryReservation.getInventory()
                        .getTotalQuantity() - inventoryReservation.getQuantity());

        //reduce reserved quantity in the inventory
        inventoryReservation.getInventory()
                .setReservedQuantity(inventoryReservation.getInventory()
                        .getReservedQuantity() - inventoryReservation.getQuantity());

        //save changes
        inventoryRepository.save(inventoryReservation.getInventory());
        inventoryReservationRepository.save(inventoryReservation);

        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }

    @Override
    public InventoryReservationDTO getReservation(Long reservationId) {
        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }
}
