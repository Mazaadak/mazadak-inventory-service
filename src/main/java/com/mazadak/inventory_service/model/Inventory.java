package com.mazadak.inventory_service.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "inventories")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Inventory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id")
    private UUID inventoryId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "total_quantity")
    private int totalQuantity;

    @Column(name = "reserved_quantity")
    private int reservedQuantity = 0;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryReservation> reservations = new ArrayList<>();

    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    public void reduceQuantity(int quantity) {
        if (quantity > totalQuantity) {
            throw new IllegalArgumentException("Not enough inventory");
        }
        totalQuantity -= quantity;
    }
}
