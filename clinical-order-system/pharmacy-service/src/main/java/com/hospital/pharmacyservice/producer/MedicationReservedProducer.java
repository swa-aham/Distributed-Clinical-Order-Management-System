package com.hospital.pharmacyservice.producer;

import com.hospital.pharmacyservice.event.MedicationReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationReservedProducer {

    private static final String TOPIC = "medication-reserved";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReservationResult(MedicationReservedEvent event) {
        log.info("Publishing MedicationReservedEvent: orderId={}, status={}, eventId={}",
                event.getOrderId(), event.getStatus(), event.getEventId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish MedicationReservedEvent for orderId={}: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            } else {
                log.info("MedicationReservedEvent published: orderId={}, partition={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
