package com.hospital.notificationservice.service;

import com.hospital.notificationservice.event.ClinicalOrderCreatedEvent;
import com.hospital.notificationservice.event.MedicationReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    // In-memory store (replace with DB or email/SMS provider in production)
    private final List<NotificationRecord> notificationLog = Collections.synchronizedList(new ArrayList<>());

    public void handleOrderCreated(ClinicalOrderCreatedEvent event) {
        String message = String.format(
                "[ORDER CREATED] orderId=%s | patientId=%s | doctorId=%s | type=%s",
                event.getOrderId(), event.getPatientId(), event.getDoctorId(), event.getOrderType()
        );
        log.info("NOTIFICATION: {}", message);
        notificationLog.add(new NotificationRecord(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                event.getOrderId(),
                message,
                LocalDateTime.now()
        ));
    }

    public void handleMedicationReserved(MedicationReservedEvent event) {
        String status = event.getStatus();
        String message;

        if ("RESERVED".equalsIgnoreCase(status)) {
            message = String.format(
                    "[MEDICATION RESERVED] orderId=%s | Medication successfully reserved.",
                    event.getOrderId()
            );
        } else {
            message = String.format(
                    "[MEDICATION FAILED] orderId=%s | Reservation failed: %s",
                    event.getOrderId(), event.getFailureReason()
            );
        }

        log.info("NOTIFICATION: {}", message);
        notificationLog.add(new NotificationRecord(
                UUID.randomUUID().toString(),
                "MEDICATION_" + status.toUpperCase(),
                event.getOrderId(),
                message,
                LocalDateTime.now()
        ));
    }

    public List<NotificationRecord> getNotificationLog() {
        return Collections.unmodifiableList(notificationLog);
    }

    public record NotificationRecord(
            String notificationId,
            String type,
            UUID orderId,
            String message,
            LocalDateTime receivedAt
    ) {}
}
