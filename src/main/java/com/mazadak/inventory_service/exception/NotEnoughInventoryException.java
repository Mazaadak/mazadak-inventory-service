package com.mazadak.inventory_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class NotEnoughInventoryException extends RuntimeException {
    
    public NotEnoughInventoryException(Long productId, int requested, int available) {
        super(String.format("Not enough inventory for product ID: %d. Requested: %d, Available: %d", 
            productId, requested, available));
    }
}
