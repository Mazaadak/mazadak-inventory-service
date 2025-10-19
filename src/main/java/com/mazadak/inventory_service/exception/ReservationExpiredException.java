package com.mazadak.inventory_service.exception;

import java.util.UUID;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException(UUID reservationId) {
        super("Reservation expired with id: " + reservationId);
    }
}
