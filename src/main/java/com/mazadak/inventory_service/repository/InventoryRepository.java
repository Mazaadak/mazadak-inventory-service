package com.mazadak.inventory_service.repository;

import com.mazadak.inventory_service.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(UUID productId);

    void deleteByProductId(UUID productId);
    
    Optional<Inventory> findByIdempotencyKey(UUID idempotencyKey);

    Boolean existsByProductId(UUID productId);
}
