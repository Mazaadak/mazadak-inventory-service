package com.mazadak.inventory_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReleaseReservationRequest(
        List<UUID> reservationIds,
       @NotNull  UUID orderId
){ }
