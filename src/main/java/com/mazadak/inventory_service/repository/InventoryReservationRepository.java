package com.mazadak.inventory_service.repository;

import com.mazadak.inventory_service.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByInventory_ProductIdAndIdempotencyKey(Long productId, UUID idempotencyKey);
}
