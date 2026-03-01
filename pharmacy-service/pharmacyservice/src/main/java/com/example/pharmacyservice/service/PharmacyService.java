package com.example.pharmacyservice.service;

import com.example.pharmacyservice.entity.Medication;
import com.example.pharmacyservice.entity.ProcessedEvent;
import com.example.pharmacyservice.entity.Reservation;
import com.example.pharmacyservice.entity.ReservationStatus;
import com.example.pharmacyservice.event.ClinicalOrderCreatedEvent;
import com.example.pharmacyservice.repository.MedicationRepository;
import com.example.pharmacyservice.repository.ProcessedEventRepository;
import com.example.pharmacyservice.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final MedicationRepository medicationRepository;
    private final ReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void handlePrescription(ClinicalOrderCreatedEvent event) {

        UUID eventId = event.getEventId();

        // 1️⃣ Idempotency
        if (processedEventRepository.existsById(eventId)) {
            return;
        }

        // 2️⃣ Get any available medication (temporary logic)
        Medication medication = medicationRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No medication available"));

        // 3️⃣ Check stock
        if (medication.getStockQuantity() <= 0) {
            publishFailure(event);
            return;
        }

        // 4️⃣ Reduce stock
        medication.setStockQuantity(medication.getStockQuantity() - 1);
        medicationRepository.save(medication);

        // 5️⃣ Create reservation
        reservationRepository.save(
                Reservation.builder()
                        .id(UUID.randomUUID())
                        .orderId(event.getPayload().getOrderId())
                        .medicationId(medication.getId())
                        .quantity(1)
                        .status(ReservationStatus.RESERVED)
                        .build()
        );

        // 6️⃣ Save processed event
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .processedAt(LocalDateTime.now())
                        .build()
        );

        // 7️⃣ Publish success
        kafkaTemplate.send("medication-reserved", event);
    }

    private void publishFailure(ClinicalOrderCreatedEvent event) {
        kafkaTemplate.send("order-failed", event);
    }
}