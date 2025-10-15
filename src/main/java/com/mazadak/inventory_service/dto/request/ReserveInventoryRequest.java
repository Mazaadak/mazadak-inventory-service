package com.mazadak.inventory_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReserveInventoryRequest(
        @NotNull Long productId,
        @NotNull Long userId,
        @Positive int quantity,
        @NotNull UUID idempotencyKey
) { }
