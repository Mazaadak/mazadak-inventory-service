package com.mazadak.inventory_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.CONFLICT)
public class NotEnoughInventoryException extends RuntimeException {
    
    public NotEnoughInventoryException(UUID productId, int requested, int available) {
        super(String.format("Not enough inventory for product ID: %s. Requested: %d, Available: %d",
            productId, requested, available));
    }
}
