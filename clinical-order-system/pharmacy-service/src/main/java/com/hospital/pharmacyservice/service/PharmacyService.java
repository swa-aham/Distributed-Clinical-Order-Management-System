package com.hospital.pharmacyservice.service;

import com.hospital.pharmacyservice.entity.*;
import com.hospital.pharmacyservice.event.ClinicalOrderCreatedEvent;
import com.hospital.pharmacyservice.event.MedicationReservedEvent;
import com.hospital.pharmacyservice.producer.MedicationReservedProducer;
import com.hospital.pharmacyservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyService {

    private static final int DEFAULT_RESERVATION_QUANTITY = 1;

    private final MedicationRepository medicationRepository;
    private final ReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final MedicationReservedProducer reservedProducer;

    /**
     * Core method: processes an incoming ClinicalOrderCreatedEvent.
     * Only handles MEDICATION order types — other types are ignored.
     * Fully idempotent via processed_events table.
     */
    @Transactional
    public void processOrderEvent(ClinicalOrderCreatedEvent event) {
        // Idempotency guard
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event skipped: eventId={}", event.getEventId());
            return;
        }

        // Only pharmacy handles MEDICATION orders
        if (!"MEDICATION".equalsIgnoreCase(event.getOrderType())) {
            log.info("Ignoring non-MEDICATION order: orderId={}, type={}", event.getOrderId(), event.getOrderType());
            markEventProcessed(event.getEventId(), "ClinicalOrderCreatedEvent");
            return;
        }

        // Prevent duplicate reservations for the same order
        if (reservationRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Reservation already exists for orderId={}, skipping", event.getOrderId());
            markEventProcessed(event.getEventId(), "ClinicalOrderCreatedEvent");
            return;
        }

        log.info("Processing MEDICATION order: orderId={}", event.getOrderId());
        attemptReservation(event);
        markEventProcessed(event.getEventId(), "ClinicalOrderCreatedEvent");
    }

    private void attemptReservation(ClinicalOrderCreatedEvent event) {
        // Pick first available medication with stock > 0
        // In a real system, the order would specify which medication
        List<Medication> available = medicationRepository.findAll().stream()
                .filter(m -> m.getStock() > 0)
                .toList();

        if (available.isEmpty()) {
            log.warn("No medication in stock for orderId={}", event.getOrderId());
            saveFailedReservation(event.getOrderId(), null, "No medication available in stock");
            publishResult(event.getOrderId(), "FAILED", "No medication available in stock");
            return;
        }

        // Acquire pessimistic lock on chosen medication
        Medication medication = medicationRepository.findByIdWithLock(available.get(0).getMedicationId())
                .orElse(null);

        if (medication == null || medication.getStock() < DEFAULT_RESERVATION_QUANTITY) {
            log.warn("Insufficient stock for orderId={}", event.getOrderId());
            saveFailedReservation(event.getOrderId(), medication, "Insufficient stock");
            publishResult(event.getOrderId(), "FAILED", "Insufficient stock");
            return;
        }

        // Deduct stock and save reservation
        medication.setStock(medication.getStock() - DEFAULT_RESERVATION_QUANTITY);
        medicationRepository.save(medication);

        Reservation reservation = Reservation.builder()
                .orderId(event.getOrderId())
                .medication(medication)
                .quantity(DEFAULT_RESERVATION_QUANTITY)
                .status(ReservationStatus.RESERVED)
                .build();
        reservationRepository.save(reservation);

        log.info("Medication reserved: orderId={}, medication={}, remainingStock={}",
                event.getOrderId(), medication.getMedicationName(), medication.getStock());

        publishResult(event.getOrderId(), "RESERVED", null);
    }

    private void saveFailedReservation(UUID orderId, Medication medication, String reason) {
        Reservation reservation = Reservation.builder()
                .orderId(orderId)
                .medication(medication)
                .quantity(0)
                .status(ReservationStatus.FAILED)
                .failureReason(reason)
                .build();
        reservationRepository.save(reservation);
    }

    private void publishResult(UUID orderId, String status, String failureReason) {
        MedicationReservedEvent event = MedicationReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .status(status)
                .failureReason(failureReason)
                .timestamp(LocalDateTime.now())
                .build();
        reservedProducer.publishReservationResult(event);
    }

    private void markEventProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(LocalDateTime.now())
                .eventType(eventType)
                .build());
    }

    // ─── Query methods ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Medication> getAllMedications() {
        return medicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reservation getReservationByOrderId(UUID orderId) {
        return reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Reservation not found for orderId: " + orderId));
    }
}
