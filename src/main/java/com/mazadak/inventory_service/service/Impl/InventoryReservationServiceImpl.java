package com.mazadak.inventory_service.service.Impl;

import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.exception.NotEnoughInventoryException;
import com.mazadak.inventory_service.exception.ReservationExpiredException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryService inventoryService;
    private final InventoryReservationMapper inventoryReservationMapper;

    @Value("${app.reservation.timeout-minutes}")
    private int reservationTimeoutMinutes;

    @Override
    @Transactional
    public InventoryReservationDTO reserveInventory(ReserveInventoryRequest request) {
        log.info("Reserving inventory for product {}", request.productId());
        Optional<InventoryReservation> inventoryReservation= inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(
                request.productId(), request.idempotencyKey());

        log.info("Found existing reservation: {}", inventoryReservation.isPresent());
        if (inventoryReservation.isPresent()) { // request has been processed
            log.info("Found existing reservation: {}", inventoryReservation.get());
            return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation.get());
        }

        Inventory inventory = inventoryService.findOrCreateInventory(request.productId());
        log.info("Found inventory: {}", inventory);

        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        log.info("Available quantity: {}", availableQuantity);
        if (availableQuantity < request.quantity()) {
            log.info("Not enough inventory");
            throw new NotEnoughInventoryException(
                inventory.getProductId(), 
                request.quantity(), 
                availableQuantity
            );
        }

        log.info("Updating inventory with idempotency key");
        inventory.setIdempotencyKey(request.idempotencyKey());

        log.info("Updating reserved quantity");
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        inventory = inventoryRepository.save(inventory);

        log.info("Creating reservation");
        InventoryReservation updatedReservation = InventoryReservation.builder()
                .userId(request.userId())
                .inventory(inventory)
                .quantity(request.quantity())
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(reservationTimeoutMinutes))
                .idempotencyKey(request.idempotencyKey())
                .build();

        log.info("Saving reservation");
        inventoryReservationRepository.save(updatedReservation);

        return inventoryReservationMapper.toInventoryReservationDTO(updatedReservation);
    }

    @Override
    @Transactional
    public InventoryReservationDTO releaseReservation(Long reservationId) {
        log.info("Releasing reservation with id: {}", reservationId);

        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                            log.error("Reservation not found with id: {}", reservationId);
                            return new ReservationNotFoundException(reservationId) ;});

        log.info("Updating reservation status");
        inventoryReservation.release();

        log.info("Updating reserved quantity");
        inventoryReservation.getInventory()
                .setReservedQuantity(inventoryReservation.getInventory()
                        .getReservedQuantity() - inventoryReservation.getQuantity());

        log.info("Saving inventory");
        inventoryRepository.save(inventoryReservation.getInventory());
        log.info("Saving reservation");
        inventoryReservationRepository.save(inventoryReservation);

        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }

    @Override
    @Transactional
    public InventoryReservationDTO confirmReservation(Long reservationId, ConfirmReservationRequest request) {
        log.info("Confirming reservation with id: {}", reservationId);
        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() ->{
                    log.error("Reservation not found with id: {}", reservationId);
                return new ReservationNotFoundException(reservationId);});

        log.info("Check for reservation expiration");
        if (inventoryReservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.info("Reservation has expired");
            releaseReservation(reservationId);
            throw new ReservationExpiredException(reservationId);
        }

        log.info("Updating reservation status");
        inventoryReservation.confirm(request.orderId());

        log.info("Updating total quantity");
        inventoryReservation.getInventory()
                        .setTotalQuantity(inventoryReservation.getInventory()
                        .getTotalQuantity() - inventoryReservation.getQuantity());

        log.info("Updating reserved quantity");
        inventoryReservation.getInventory()
                .setReservedQuantity(inventoryReservation.getInventory()
                        .getReservedQuantity() - inventoryReservation.getQuantity());

        log.info("Saving inventory");
        inventoryRepository.save(inventoryReservation.getInventory());
        log.info("Saving reservation");
        inventoryReservationRepository.save(inventoryReservation);

        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }

    @Override
    public InventoryReservationDTO getReservation(Long reservationId) {
        log.info("Getting reservation with id: {}", reservationId);
        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.error("Reservation not found with id: {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });
        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }
}
