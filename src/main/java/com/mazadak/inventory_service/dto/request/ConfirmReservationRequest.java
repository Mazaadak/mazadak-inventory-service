package com.mazadak.inventory_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmReservationRequest(
        @NotNull Long orderId) { }
