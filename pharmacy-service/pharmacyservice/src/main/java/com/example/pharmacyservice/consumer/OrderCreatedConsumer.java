package com.example.pharmacyservice.consumer;

import com.example.pharmacyservice.event.ClinicalOrderCreatedEvent;
import com.example.pharmacyservice.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PharmacyService pharmacyService;

    @KafkaListener(
            topics = "clinical-order-created",
            groupId = "pharmacy-group"
    )
    public void listen(ClinicalOrderCreatedEvent event) {

        if (event.getPayload().getOrderType().name().equals("PRESCRIPTION")) {
            pharmacyService.handlePrescription(event);
        }
    }
}