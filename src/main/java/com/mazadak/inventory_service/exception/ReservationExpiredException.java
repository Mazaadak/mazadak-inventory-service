package com.mazadak.inventory_service.exception;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException(Long reservationId) {
        super("Reservation expired with id: " + reservationId);
    }
}
