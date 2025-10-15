package com.mazadak.inventory_service.repository;

import com.mazadak.inventory_service.model.InventoryReservation;
import com.mazadak.inventory_service.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {


    @Query("SELECT r FROM InventoryReservation r WHERE r.status = :status AND r.expiresAt < :expiresAt")
    List<InventoryReservation> findExpiredReservations(
            @Param("status") ReservationStatus status,
            @Param("expiresAt") LocalDateTime expiresAt
    );
    Optional<InventoryReservation> findByInventory_ProductIdAndIdempotencyKey(Long productId, UUID idempotencyKey);
}
