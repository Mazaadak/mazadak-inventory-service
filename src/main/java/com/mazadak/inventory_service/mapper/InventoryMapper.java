package com.mazadak.inventory_service.mapper;


import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.model.Inventory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    InventoryDTO toInventoryDTO(Inventory inventory);
    Inventory toInventory(InventoryDTO inventoryDTO);
}
