package com.mazadak.inventory_service.service.Impl;

import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.request.reserveItemDTO;
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
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryService inventoryService;
    private final InventoryReservationMapper inventoryReservationMapper;

    @Value("${app.reservation.timeout-minutes:15}")
    private int reservationTimeoutMinutes;

    @Override
    @Transactional
    public List<UUID> reserveInventory(UUID idempotencyKey, ReserveInventoryRequest request) {
        List<reserveItemDTO> items = request.items();
        UUID orderId = request.orderId();
        List<UUID> reservations = new ArrayList<>();

        for (reserveItemDTO requestItem : items) {

            log.info("Reserving inventory for product {}", requestItem.productId());
            
            Optional<InventoryReservation> inventoryReservation = inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(
                    requestItem.productId(), idempotencyKey);

            log.info("Found existing reservation: {}", inventoryReservation.isPresent());
            if (inventoryReservation.isPresent()) { // request has been processed
                log.info("Request has been processed");
                return null;
            }

            Inventory inventory = inventoryService.findOrCreateInventory(requestItem.productId());
            log.info("Found inventory: {}", inventory);

            int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
            log.info("Available quantity: {}", availableQuantity);
            if (availableQuantity < requestItem.quantity()) {
                log.info("Not enough inventory");
                throw new NotEnoughInventoryException(
                        inventory.getProductId(),
                        requestItem.quantity(),
                        availableQuantity
                );
            }

            log.info("Updating inventory with idempotency key");
            inventory.setIdempotencyKey(idempotencyKey);

            log.info("Updating reserved quantity");
            inventory.setReservedQuantity(inventory.getReservedQuantity() + requestItem.quantity());
            inventory = inventoryRepository.save(inventory);

            log.info("Creating reservation");
            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(orderId)
                    .inventory(inventory)
                    .quantity(requestItem.quantity())
                    .status(ReservationStatus.RESERVED)
                    .expiresAt(LocalDateTime.now().plusMinutes(reservationTimeoutMinutes))
                    .idempotencyKey(idempotencyKey)
                    .build();

            InventoryReservation needed =  inventoryReservationRepository.save(reservation);
            log.info("Saving reservation {}", needed.getInventoryReservationId());
            reservations.add(needed.getInventoryReservationId());
        }
        return reservations;
    }

    @Override
    @Transactional
    public List<InventoryReservationDTO> releaseReservation(UUID idempotencyKey, List<UUID> reservations) {
        List<InventoryReservationDTO> releasedReservations = new ArrayList<>();
        for (UUID reservationId : reservations) {
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

        releasedReservations.add(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation));
        }
        return releasedReservations;
    }

    @Override
    @Transactional
    public List<InventoryReservationDTO> confirmReservation(UUID idempotencyKey, ConfirmReservationRequest request) {
        List<UUID> reservationIds = request.reservationIds();
        UUID orderId = request.orderId();
        List<InventoryReservationDTO> confirmedReservations = new ArrayList<>();
        for (UUID reservationId : reservationIds) {
            log.info("Confirming reservation with id: {}", reservationId);
            InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                    .orElseThrow(() -> {
                        log.error("Reservation not found with id: {}", reservationId);
                        return new ReservationNotFoundException(reservationId);
                    });

            log.info("Check for reservation expiration");
            if (inventoryReservation.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.info("Reservation has expired");
                releaseReservation(idempotencyKey, Collections.singletonList(reservationId));
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
            confirmedReservations.add(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation));
        }
        return confirmedReservations;
    }

    @Override
    public InventoryReservationDTO getReservation(UUID reservationId) {
        log.info("Getting reservation with id: {}", reservationId);
        InventoryReservation inventoryReservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.error("Reservation not found with id: {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });
        return inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation);
    }
}
