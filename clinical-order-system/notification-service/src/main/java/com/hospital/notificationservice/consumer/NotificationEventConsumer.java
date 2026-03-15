package com.hospital.notificationservice.consumer;

import com.hospital.notificationservice.event.ClinicalOrderCreatedEvent;
import com.hospital.notificationservice.event.MedicationReservedEvent;
import com.hospital.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "clinical-order-created",
            groupId = "notification-order-group",
            containerFactory = "orderCreatedListenerFactory"
    )
    public void onOrderCreated(ClinicalOrderCreatedEvent event) {
        log.info("Notification received ClinicalOrderCreatedEvent: orderId={}", event.getOrderId());
        notificationService.handleOrderCreated(event);
    }

    @KafkaListener(
            topics = "medication-reserved",
            groupId = "notification-medication-group",
            containerFactory = "medicationReservedListenerFactory"
    )
    public void onMedicationReserved(MedicationReservedEvent event) {
        log.info("Notification received MedicationReservedEvent: orderId={}, status={}",
                event.getOrderId(), event.getStatus());
        notificationService.handleMedicationReserved(event);
    }
}
