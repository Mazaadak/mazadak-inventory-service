package com.mazadak.inventory_service.service.Impl;

import com.mazadak.common.exception.domain.inventory.NotEnoughStockException;
import com.mazadak.common.exception.domain.inventory.ReservationExpiredException;
import com.mazadak.common.exception.shared.ResourceNotFoundException;
import com.mazadak.inventory_service.dto.request.ConfirmReservationRequest;
import com.mazadak.inventory_service.dto.request.ReserveInventoryRequest;
import com.mazadak.inventory_service.dto.request.reserveItemDTO;
import com.mazadak.inventory_service.dto.response.InventoryReservationDTO;
import com.mazadak.inventory_service.mapper.InventoryReservationMapper;
import com.mazadak.inventory_service.model.Inventory;
import com.mazadak.inventory_service.model.InventoryReservation;
import com.mazadak.inventory_service.model.enums.ReservationStatus;
import com.mazadak.inventory_service.repository.InventoryRepository;
import com.mazadak.inventory_service.repository.InventoryReservationRepository;
import com.mazadak.inventory_service.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryReservationService Tests")
class InventoryReservationServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryReservationMapper inventoryReservationMapper;

    @InjectMocks
    private InventoryReservationServiceImpl inventoryReservationService;

    private UUID productId;
    private UUID orderId;
    private UUID idempotencyKey;
    private UUID reservationId;
    private Inventory inventory;
    private InventoryReservation inventoryReservation;
    private InventoryReservationDTO inventoryReservationDTO;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        inventory = new Inventory();
        inventory.setInventoryId(UUID.randomUUID());
        inventory.setProductId(productId);
        inventory.setTotalQuantity(100);
        inventory.setReservedQuantity(20);
        inventory.setDeleted(false);
        inventory.setReservations(new ArrayList<>());


        inventoryReservation = InventoryReservation.builder()
                .inventoryReservationId(reservationId)
                .orderId(orderId)
                .inventory(inventory)
                .quantity(10)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .idempotencyKey(idempotencyKey)
                .build();

        inventoryReservationDTO = new InventoryReservationDTO(
                reservationId,
                productId,
                10,
                ReservationStatus.RESERVED
        );
    }

    @Nested
    @DisplayName("ReserveInventory Tests")
    class ReserveInventoryTests {

        private reserveItemDTO reserveItem;
        private ReserveInventoryRequest reserveRequest;

        @BeforeEach
        void setUp() {
            reserveItem = new reserveItemDTO(productId, 10);
            reserveRequest = new ReserveInventoryRequest(List.of(reserveItem), orderId);
        }

        @Test
        @DisplayName("Should reserve inventory successfully for single item")
        void shouldReserveInventorySuccessfullyForSingleItem() {
            // Arrange
            when(inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(productId, idempotencyKey))
                    .thenReturn(Optional.empty());
            when(inventoryService.findOrCreateInventory(productId)).thenReturn(inventory);
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryReservationRepository.save(any(InventoryReservation.class))).thenReturn(inventoryReservation);

            // Act
            List<UUID> result = inventoryReservationService.reserveInventory(idempotencyKey, reserveRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(reservationId);
            assertThat(inventory.getReservedQuantity()).isEqualTo(30); // 20 + 10
            assertThat(inventory.getIdempotencyKey()).isEqualTo(idempotencyKey);

            ArgumentCaptor<InventoryReservation> reservationCaptor = ArgumentCaptor.forClass(InventoryReservation.class);
            verify(inventoryReservationRepository).save(reservationCaptor.capture());

            InventoryReservation savedReservation = reservationCaptor.getValue();
            assertThat(savedReservation.getOrderId()).isEqualTo(orderId);
            assertThat(savedReservation.getQuantity()).isEqualTo(10);
            assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(savedReservation.getIdempotencyKey()).isEqualTo(idempotencyKey);
        }

        @Test
        @DisplayName("Should return null when reservation already exists (idempotency)")
        void shouldReturnNullWhenReservationAlreadyExists() {
            // Arrange
            when(inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(productId, idempotencyKey))
                    .thenReturn(Optional.of(inventoryReservation));

            // Act
            List<UUID> result = inventoryReservationService.reserveInventory(idempotencyKey, reserveRequest);

            // Assert
            assertThat(result).isNull();
            verify(inventoryService, never()).findOrCreateInventory(any());
            verify(inventoryRepository, never()).save(any());
            verify(inventoryReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when not enough stock available")
        void shouldThrowExceptionWhenNotEnoughStockAvailable() {
            // Arrange
            inventory.setTotalQuantity(100);
            inventory.setReservedQuantity(95);
            reserveItemDTO item = new reserveItemDTO(productId, 10);
            ReserveInventoryRequest request = new ReserveInventoryRequest(List.of(item), orderId);

            when(inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(productId, idempotencyKey))
                    .thenReturn(Optional.empty());
            when(inventoryService.findOrCreateInventory(productId)).thenReturn(inventory);

            // Act & Assert
            assertThatThrownBy(() -> inventoryReservationService.reserveInventory(idempotencyKey, request))
                    .isInstanceOf(NotEnoughStockException.class);

            verify(inventoryRepository, never()).save(any());
            verify(inventoryReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reserve inventory for multiple items")
        void shouldReserveInventoryForMultipleItems() {
            // Arrange
            UUID productId2 = UUID.randomUUID();
            UUID reservationId2 = UUID.randomUUID();

            Inventory inventory2 = new Inventory();
            inventory2.setInventoryId(UUID.randomUUID());
            inventory2.setProductId(productId2);
            inventory2.setTotalQuantity(50);
            inventory2.setReservedQuantity(10);
            inventory2.setDeleted(false);

            InventoryReservation reservation2 = InventoryReservation.builder()
                    .inventoryReservationId(reservationId2)
                    .orderId(orderId)
                    .inventory(inventory2)
                    .quantity(5)
                    .status(ReservationStatus.RESERVED)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .idempotencyKey(idempotencyKey)
                    .build();

            reserveItemDTO item1 = new reserveItemDTO(productId, 10);
            reserveItemDTO item2 = new reserveItemDTO(productId2, 5);
            ReserveInventoryRequest multiItemRequest = new ReserveInventoryRequest(List.of(item1, item2), orderId);

            when(inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(productId, idempotencyKey))
                    .thenReturn(Optional.empty());
            when(inventoryReservationRepository.findByInventory_ProductIdAndIdempotencyKey(productId2, idempotencyKey))
                    .thenReturn(Optional.empty());
            when(inventoryService.findOrCreateInventory(productId)).thenReturn(inventory);
            when(inventoryService.findOrCreateInventory(productId2)).thenReturn(inventory2);
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryRepository.save(inventory2)).thenReturn(inventory2);
            when(inventoryReservationRepository.save(any(InventoryReservation.class)))
                    .thenReturn(inventoryReservation, reservation2);

            // Act
            List<UUID> result = inventoryReservationService.reserveInventory(idempotencyKey, multiItemRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(inventory.getReservedQuantity()).isEqualTo(30);
            assertThat(inventory2.getReservedQuantity()).isEqualTo(15);
            verify(inventoryReservationRepository, times(2)).save(any(InventoryReservation.class));
        }
    }

    @Nested
    @DisplayName("ReleaseReservation Tests")
    class ReleaseReservationTests {

        @Test
        @DisplayName("Should release single reservation successfully")
        void shouldReleaseSingleReservationSuccessfully() {
            // Arrange
            inventory.setReservedQuantity(30);
            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.of(inventoryReservation));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryReservationRepository.save(inventoryReservation)).thenReturn(inventoryReservation);
            when(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation))
                    .thenReturn(inventoryReservationDTO);

            // Act
            List<InventoryReservationDTO> result = inventoryReservationService.releaseReservation(
                    idempotencyKey, List.of(reservationId));

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(inventory.getReservedQuantity()).isEqualTo(20); // 30 - 10
            verify(inventoryRepository).save(inventory);
            verify(inventoryReservationRepository).save(inventoryReservation);
        }

        @Test
        @DisplayName("Should throw exception when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            // Arrange
            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryReservationService.releaseReservation(
                    idempotencyKey, List.of(reservationId)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation")
                    .hasMessageContaining(reservationId.toString());

            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should release multiple reservations")
        void shouldReleaseMultipleReservations() {
            // Arrange
            UUID reservationId2 = UUID.randomUUID();
            InventoryReservation reservation2 = InventoryReservation.builder()
                    .inventoryReservationId(reservationId2)
                    .orderId(orderId)
                    .inventory(inventory)
                    .quantity(5)
                    .status(ReservationStatus.RESERVED)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .idempotencyKey(idempotencyKey)
                    .build();

            InventoryReservationDTO dto2 = new InventoryReservationDTO(
                    reservationId2, productId, 5, ReservationStatus.RELEASED);

            inventory.setReservedQuantity(35);

            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.of(inventoryReservation));
            when(inventoryReservationRepository.findById(reservationId2)).thenReturn(Optional.of(reservation2));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryReservationRepository.save(any(InventoryReservation.class)))
                    .thenReturn(inventoryReservation, reservation2);
            when(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation))
                    .thenReturn(inventoryReservationDTO);
            when(inventoryReservationMapper.toInventoryReservationDTO(reservation2)).thenReturn(dto2);

            // Act
            List<InventoryReservationDTO> result = inventoryReservationService.releaseReservation(
                    idempotencyKey, List.of(reservationId, reservationId2));

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(inventory.getReservedQuantity()).isEqualTo(20); // 35 - 10 - 5
            verify(inventoryReservationRepository, times(2)).save(any(InventoryReservation.class));
        }
    }

    @Nested
    @DisplayName("ConfirmReservation Tests")
    class ConfirmReservationTests {

        private ConfirmReservationRequest confirmRequest;

        @BeforeEach
        void setUp() {
            confirmRequest = new ConfirmReservationRequest(List.of(reservationId), orderId);
        }

        @Test
        @DisplayName("Should confirm reservation successfully")
        void shouldConfirmReservationSuccessfully() {
            // Arrange
            inventory.setTotalQuantity(100);
            inventory.setReservedQuantity(30);
            inventoryReservation.setQuantity(10);

            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.of(inventoryReservation));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryReservationRepository.save(inventoryReservation)).thenReturn(inventoryReservation);
            when(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation))
                    .thenReturn(inventoryReservationDTO);

            // Act
            List<InventoryReservationDTO> result = inventoryReservationService.confirmReservation(
                    idempotencyKey, confirmRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(inventory.getTotalQuantity()).isEqualTo(90); // 100 - 10
            assertThat(inventory.getReservedQuantity()).isEqualTo(20); // 30 - 10
            verify(inventoryRepository).save(inventory);
            verify(inventoryReservationRepository).save(inventoryReservation);
        }

        @Test
        @DisplayName("Should throw exception when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            // Arrange
            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryReservationService.confirmReservation(idempotencyKey, confirmRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation")
                    .hasMessageContaining(reservationId.toString());

            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception and release when reservation expired")
        void shouldThrowExceptionAndReleaseWhenReservationExpired() {
            // Arrange
            inventoryReservation.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired

            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.of(inventoryReservation));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryReservationRepository.save(inventoryReservation)).thenReturn(inventoryReservation);
            when(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation))
                    .thenReturn(inventoryReservationDTO);

            // Act & Assert
            assertThatThrownBy(() -> inventoryReservationService.confirmReservation(idempotencyKey, confirmRequest))
                    .isInstanceOf(ReservationExpiredException.class);

            // Verify release was called
            verify(inventoryReservationRepository, times(2)).findById(reservationId);
        }

        @Test
        @DisplayName("Should confirm multiple reservations")
        void shouldConfirmMultipleReservations() {
            // Arrange
            UUID reservationId2 = UUID.randomUUID();
            InventoryReservation reservation2 = InventoryReservation.builder()
                    .inventoryReservationId(reservationId2)
                    .orderId(orderId)
                    .inventory(inventory)
                    .quantity(15)
                    .status(ReservationStatus.RESERVED)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .idempotencyKey(idempotencyKey)
                    .build();

            InventoryReservationDTO dto2 = new InventoryReservationDTO(
                    reservationId2, productId, 15, ReservationStatus.CONFIRMED);

            ConfirmReservationRequest multiConfirmRequest = new ConfirmReservationRequest(
                    List.of(reservationId, reservationId2), orderId);

            inventory.setTotalQuantity(100);
            inventory.setReservedQuantity(40);

            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.of(inventoryReservation));
            when(inventoryReservationRepository.findById(reservationId2)).thenReturn(Optional.of(reservation2));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);
            when(inventoryReservationRepository.save(any(InventoryReservation.class)))
                    .thenReturn(inventoryReservation, reservation2);
            when(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation))
                    .thenReturn(inventoryReservationDTO);
            when(inventoryReservationMapper.toInventoryReservationDTO(reservation2)).thenReturn(dto2);

            // Act
            List<InventoryReservationDTO> result = inventoryReservationService.confirmReservation(
                    idempotencyKey, multiConfirmRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(inventory.getTotalQuantity()).isEqualTo(75); // 100 - 10 - 15
            assertThat(inventory.getReservedQuantity()).isEqualTo(15); // 40 - 10 - 15
            verify(inventoryReservationRepository, times(2)).save(any(InventoryReservation.class));
        }

    }

    @Nested
    @DisplayName("GetReservation Tests")
    class GetReservationTests {

        @Test
        @DisplayName("Should return reservation when found")
        void shouldReturnReservationWhenFound() {
            // Arrange
            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.of(inventoryReservation));
            when(inventoryReservationMapper.toInventoryReservationDTO(inventoryReservation))
                    .thenReturn(inventoryReservationDTO);

            // Act
            InventoryReservationDTO result = inventoryReservationService.getReservation(reservationId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.inventoryReservationId()).isEqualTo(reservationId);
            assertThat(result.productId()).isEqualTo(productId);
            assertThat(result.quantity()).isEqualTo(10);
            verify(inventoryReservationRepository).findById(reservationId);
            verify(inventoryReservationMapper).toInventoryReservationDTO(inventoryReservation);
        }

        @Test
        @DisplayName("Should throw exception when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            // Arrange
            when(inventoryReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> inventoryReservationService.getReservation(reservationId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation")
                    .hasMessageContaining(reservationId.toString());

            verify(inventoryReservationMapper, never()).toInventoryReservationDTO(any());
        }
    }
}