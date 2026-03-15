package com.hospital.pharmacyservice.consumer;

import com.hospital.pharmacyservice.event.ClinicalOrderCreatedEvent;
import com.hospital.pharmacyservice.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalOrderCreatedEventConsumer {

    private final PharmacyService pharmacyService;

    @KafkaListener(
            topics = "clinical-order-created",
            groupId = "pharmacy-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ClinicalOrderCreatedEvent event) {
        log.info("Received ClinicalOrderCreatedEvent: orderId={}, type={}, eventId={}",
                event.getOrderId(), event.getOrderType(), event.getEventId());
        try {
            pharmacyService.processOrderEvent(event);
        } catch (Exception e) {
            log.error("Error processing ClinicalOrderCreatedEvent for orderId={}: {}",
                    event.getOrderId(), e.getMessage(), e);
            // In production: send to Dead Letter Queue
            throw e;
        }
    }
}
