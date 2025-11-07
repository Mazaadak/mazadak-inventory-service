package com.mazadak.inventory_service.model;

import com.mazadak.inventory_service.model.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "inventory_reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "inventory")
@EqualsAndHashCode(exclude = "inventory")
public class InventoryReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_reservation_id", nullable = false, updatable = false)
    private UUID inventoryReservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    @NotNull(message = "Inventory is required")
    private Inventory inventory;

    @Min(1)
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;




    public void confirm(UUID orderId) {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED reservations can be confirmed");
        }
        this.orderId = orderId;
        this.status = ReservationStatus.CONFIRMED;
    }

    public void complete() {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED reservations can be completed");
        }
        this.completedAt = LocalDateTime.now();
        this.status = ReservationStatus.COMPLETED;
    }

    public void release() {
        if (this.status != ReservationStatus.RESERVED && this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only RESERVED or CONFIRMED reservations can be released");
        }
        this.releasedAt = LocalDateTime.now();
        this.status = ReservationStatus.RELEASED;
    }

    public void expire() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED reservations can expire");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public void fail(String reason) {
        this.failedAt = LocalDateTime.now();
        this.status = ReservationStatus.FAILED;
    }
}