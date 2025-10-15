package com.mazadak.inventory_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservationNotFoundException extends RuntimeException {
    
    public ReservationNotFoundException(Long reservationId) {
        super(String.format("Reservation not found with ID: %d", reservationId));
    }

}
