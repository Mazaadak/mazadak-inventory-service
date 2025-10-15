package com.mazadak.inventory_service.model.enums;

/**
 * Represents the lifecycle of an inventory reservation.
 * - RESERVED: Initial state when inventory is reserved
 * - CONFIRMED: Reservation is confirmed (order placed)
 * - COMPLETED: Order was fulfilled
 * - RELEASED: Inventory was released (order cancelled)
 * - EXPIRED: Reservation expired before confirmation
 * - FAILED: Reservation failed due to system error
 */
public enum ReservationStatus {
    RESERVED,
    CONFIRMED,
    COMPLETED,
    RELEASED,
    EXPIRED,
    FAILED
}
