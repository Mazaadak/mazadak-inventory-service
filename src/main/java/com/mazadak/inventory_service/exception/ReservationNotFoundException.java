package com.mazadak.inventory_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservationNotFoundException extends RuntimeException {
    
    public ReservationNotFoundException(UUID reservationId) {
        super(String.format("Reservation not found with ID: %s", reservationId));
    }

}
