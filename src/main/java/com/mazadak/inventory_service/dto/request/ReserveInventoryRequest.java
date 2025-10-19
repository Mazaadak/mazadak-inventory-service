package com.mazadak.inventory_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(
        List<reserveItemDTO> items,
        @NotNull UUID orderId
) { }
