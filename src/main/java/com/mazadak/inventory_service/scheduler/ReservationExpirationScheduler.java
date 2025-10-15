package com.mazadak.inventory_service.scheduler;

import com.mazadak.inventory_service.model.InventoryReservation;
import com.mazadak.inventory_service.model.enums.ReservationStatus;
import com.mazadak.inventory_service.repository.InventoryReservationRepository;
import com.mazadak.inventory_service.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryReservationService reservationService;

    @Scheduled(fixedRateString = "${app.reservation.cleanup-interval-ms}")
    @Transactional
    public void releaseExpiredReservations() {
        log.info("Starting expired reservations cleanup at: {}", LocalDateTime.now());

        List<InventoryReservation> expiredReservations = reservationRepository.findExpiredReservations(
                ReservationStatus.RESERVED,
                LocalDateTime.now()
        );

        log.info("Found {} expired reservations to process", expiredReservations.size());

        for (var reservation : expiredReservations) {
            try {
                log.info("Processing reservation ID: {}, expires at: {}",
                        reservation.getInventoryReservationId(),
                        reservation.getExpiresAt());
                reservationService.releaseReservation(reservation.getInventoryReservationId());
                log.info("Successfully released reservation ID: {}", reservation.getInventoryReservationId());
            } catch (Exception e) {
                log.error("Failed to release reservation ID: {}", reservation.getInventoryReservationId(), e);
            }
        }
    }
}