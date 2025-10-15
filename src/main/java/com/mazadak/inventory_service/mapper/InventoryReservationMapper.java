package com.mazadak.inventory_service.mapper;

import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.model.InventoryReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryReservationMapper {
    @Mapping(target = "productId", source = "inventory.productId")
    InventoryReservationDTO toInventoryReservationDTO(InventoryReservation inventoryReservation);

    InventoryReservation toInventoryReservation(InventoryReservationDTO inventoryReservationDTO);
}
