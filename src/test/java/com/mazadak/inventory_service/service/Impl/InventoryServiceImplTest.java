package com.mazadak.inventory_service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazadak.common.exception.domain.inventory.NotEnoughStockException;
import com.mazadak.common.exception.shared.ResourceNotFoundException;
import com.mazadak.inventory_service.dto.event.InventoryDeletedEvent;
import com.mazadak.inventory_service.dto.request.AddInventoryRequest;
import com.mazadak.inventory_service.dto.request.UpdateInventoryRequest;
import com.mazadak.inventory_service.dto.response.InventoryDTO;
import com.mazadak.inventory_service.mapper.InventoryMapper;
import com.mazadak.inventory_service.model.Inventory;
import com.mazadak.inventory_service.model.OutboxEvent;
import com.mazadak.inventory_service.repository.InventoryRepository;
import com.mazadak.inventory_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Tests")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private UUID productId;
    private UUID idempotencyKey;
    private Inventory inventory;
    private InventoryDTO inventoryDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();

        inventory = new Inventory();
        inventory.setInventoryId(UUID.randomUUID());
        inventory.setProductId(productId);
        inventory.setTotalQuantity(100);
        inventory.setReservedQuantity(20);
        inventory.setDeleted(false);
        inventory.setReservations(new ArrayList<>());

        inventoryDTO = new InventoryDTO(
                productId,
                100,
                20
        );
    }

    @Nested
    @DisplayName("FindInventoryByProductId Tests")
    class FindInventoryByProductIdTests {

        @Test
        @DisplayName("Should return inventory when product exists")
        void shouldReturnInventoryWhenProductExists() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            // Act
            Inventory result = inventoryService.findInventoryByProductId(productId);

            // Assert
            assertThat(result).isEqualTo(inventory);
            verify(inventoryRepository).findByProductId(productId);
        }

        @Test
        @DisplayName("Should throw exception when inventory not found")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.findInventoryByProductId(productId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory")
                    .hasMessageContaining(productId.toString());

            verify(inventoryRepository).findByProductId(productId);
        }
    }

    @Nested
    @DisplayName("FindOrCreateInventory Tests")
    class FindOrCreateInventoryTests {

        @Test
        @DisplayName("Should return existing inventory when found")
        void shouldReturnExistingInventoryWhenFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            // Act
            Inventory result = inventoryService.findOrCreateInventory(productId);

            // Assert
            assertThat(result).isEqualTo(inventory);
            verify(inventoryRepository).findByProductId(productId);
        }

        @Test
        @DisplayName("Should create new inventory when not found")
        void shouldCreateNewInventoryWhenNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act
            Inventory result = inventoryService.findOrCreateInventory(productId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getTotalQuantity()).isZero();
            assertThat(result.getReservedQuantity()).isZero();
            verify(inventoryRepository).findByProductId(productId);
        }
    }

    @Nested
    @DisplayName("AddInventory Tests")
    class AddInventoryTests {

        private AddInventoryRequest addRequest;

        @BeforeEach
        void setUp() {
            addRequest = new AddInventoryRequest(productId, 50);
        }

        @Test
        @DisplayName("Should return existing inventory when idempotency key exists")
        void shouldReturnExistingInventoryWhenIdempotencyKeyExists() {
            // Arrange
            when(inventoryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(inventory));
            when(inventoryMapper.toInventoryDTO(inventory)).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.addInventory(idempotencyKey, addRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.productId()).isEqualTo(productId);
            verify(inventoryRepository).findByIdempotencyKey(idempotencyKey);
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should add inventory to existing product")
        void shouldAddInventoryToExistingProduct() {
            // Arrange
            when(inventoryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryMapper.toInventoryDTO(inventory)).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.addInventory(idempotencyKey, addRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(inventory.getTotalQuantity()).isEqualTo(150); // 100 + 50
            assertThat(inventory.getIdempotencyKey()).isEqualTo(idempotencyKey);
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("Should create new inventory for new product")
        void shouldCreateNewInventoryForNewProduct() {
            // Arrange
            when(inventoryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(inventoryMapper.toInventoryDTO(any(Inventory.class))).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.addInventory(idempotencyKey, addRequest);

            // Assert
            assertThat(result).isNotNull();
            ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(inventoryCaptor.capture());

            Inventory savedInventory = inventoryCaptor.getValue();
            assertThat(savedInventory.getProductId()).isEqualTo(productId);
            assertThat(savedInventory.getIdempotencyKey()).isEqualTo(idempotencyKey);
        }

        @Test
        @DisplayName("Should restore deleted inventory when adding to deleted product")
        void shouldRestoreDeletedInventoryWhenAddingToDeletedProduct() {
            // Arrange
            inventory.setDeleted(true);
            inventory.setTotalQuantity(50);
            inventory.setReservedQuantity(10);

            when(inventoryRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryMapper.toInventoryDTO(inventory)).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.addInventory(idempotencyKey, addRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(inventory.isDeleted()).isFalse();
            assertThat(inventory.getTotalQuantity()).isEqualTo(50);
            assertThat(inventory.getReservedQuantity()).isZero();
            assertThat(inventory.getReservations()).isEmpty();
            verify(inventoryRepository).save(inventory);
        }
    }

    @Nested
    @DisplayName("GetInventory Tests")
    class GetInventoryTests {

        @Test
        @DisplayName("Should return inventory DTO for existing product")
        void shouldReturnInventoryDTOForExistingProduct() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryMapper.toInventoryDTO(inventory)).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.getInventory(productId);

            // Assert
            assertThat(result).isEqualTo(inventoryDTO);
            verify(inventoryRepository).findByProductId(productId);
            verify(inventoryMapper).toInventoryDTO(inventory);
        }

        @Test
        @DisplayName("Should throw exception when inventory not found")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.getInventory(productId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory");
        }
    }

    @Nested
    @DisplayName("ReduceQuantity Tests")
    class ReduceQuantityTests {

        @Test
        @DisplayName("Should reduce quantity successfully")
        void shouldReduceQuantitySuccessfully() {
            // Arrange
            int quantityToReduce = 30;
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryMapper.toInventoryDTO(inventory)).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.reduceQuantity(productId, quantityToReduce);

            // Assert
            assertThat(result).isNotNull();
            verify(inventoryRepository).save(inventory);
            verify(inventoryMapper).toInventoryDTO(inventory);
        }

        @Test
        @DisplayName("Should throw exception when inventory not found")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.reduceQuantity(productId, 10))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory")
                    .hasMessageContaining(productId.toString());
        }
    }

    @Nested
    @DisplayName("DeleteInventory Tests")
    class DeleteInventoryTests {

        @Test
        @DisplayName("Should soft delete inventory and create outbox event")
        void shouldSoftDeleteInventoryAndCreateOutboxEvent() throws JsonProcessingException {
            // Arrange
            String eventJson = "{\"productId\":\"" + productId + "\"}";
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(objectMapper.writeValueAsString(any(InventoryDeletedEvent.class))).thenReturn(eventJson);
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            // Act
            inventoryService.deleteInventory(productId);

            // Assert
            assertThat(inventory.isDeleted()).isTrue();

            ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(outboxCaptor.capture());

            OutboxEvent savedEvent = outboxCaptor.getValue();
            assertThat(savedEvent.getAggregateType()).isEqualTo("Inventory");
            assertThat(savedEvent.getEventType()).isEqualTo("InventoryDeleted");
            assertThat(savedEvent.getPayload()).isEqualTo(eventJson);

            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("Should throw exception when inventory not found")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.deleteInventory(productId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory");

            verify(outboxEventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("ExistsByProductId Tests")
    class ExistsByProductIdTests {

        @Test
        @DisplayName("Should return true when inventory exists and not deleted")
        void shouldReturnTrueWhenInventoryExistsAndNotDeleted() {
            // Arrange
            when(inventoryRepository.existsByProductIdAndDeletedFalse(productId)).thenReturn(true);

            // Act
            Boolean result = inventoryService.existsByProductId(productId);

            // Assert
            assertThat(result).isTrue();
            verify(inventoryRepository).existsByProductIdAndDeletedFalse(productId);
        }

        @Test
        @DisplayName("Should return false when inventory does not exist")
        void shouldReturnFalseWhenInventoryDoesNotExist() {
            // Arrange
            when(inventoryRepository.existsByProductIdAndDeletedFalse(productId)).thenReturn(false);

            // Act
            Boolean result = inventoryService.existsByProductId(productId);

            // Assert
            assertThat(result).isFalse();
            verify(inventoryRepository).existsByProductIdAndDeletedFalse(productId);
        }

        @Test
        @DisplayName("Should return false when inventory is deleted")
        void shouldReturnFalseWhenInventoryIsDeleted() {
            // Arrange
            when(inventoryRepository.existsByProductIdAndDeletedFalse(productId)).thenReturn(false);

            // Act
            Boolean result = inventoryService.existsByProductId(productId);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("RestoreInventory Tests")
    class RestoreInventoryTests {

        @Test
        @DisplayName("Should restore deleted inventory")
        void shouldRestoreDeletedInventory() {
            // Arrange
            inventory.setDeleted(true);
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            // Act
            inventoryService.restoreInventory(productId);

            // Assert
            assertThat(inventory.isDeleted()).isFalse();
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("Should throw exception when inventory not found")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.restoreInventory(productId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory")
                    .hasMessageContaining(productId.toString());
        }
    }

    @Nested
    @DisplayName("UpdateInventory Tests")
    class UpdateInventoryTests {

        private UpdateInventoryRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = new UpdateInventoryRequest(150);
        }

        @Test
        @DisplayName("Should update inventory quantity successfully")
        void shouldUpdateInventoryQuantitySuccessfully() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryMapper.toInventoryDTO(inventory)).thenReturn(inventoryDTO);

            // Act
            InventoryDTO result = inventoryService.updateInventory(productId, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(inventory.getTotalQuantity()).isEqualTo(150);
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("Should throw exception when inventory not found")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.updateInventory(productId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory");

            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when new quantity is less than reserved quantity")
        void shouldThrowExceptionWhenNewQuantityLessThanReservedQuantity() {
            // Arrange
            inventory.setTotalQuantity(100);
            inventory.setReservedQuantity(50);
            UpdateInventoryRequest invalidRequest = new UpdateInventoryRequest(30); // Less than reserved (50)

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            // Act & Assert
            assertThatThrownBy(() -> inventoryService.updateInventory(productId, invalidRequest))
                    .isInstanceOf(NotEnoughStockException.class);

            verify(inventoryRepository, never()).save(any());
        }
    }
}