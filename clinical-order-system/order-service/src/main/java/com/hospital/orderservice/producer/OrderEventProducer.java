package com.hospital.orderservice.producer;

import com.hospital.orderservice.event.ClinicalOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String TOPIC_ORDER_CREATED = "clinical-order-created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreatedEvent(ClinicalOrderCreatedEvent event) {
        log.info("Publishing ClinicalOrderCreatedEvent: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC_ORDER_CREATED, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish ClinicalOrderCreatedEvent for orderId={}: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            } else {
                log.info("ClinicalOrderCreatedEvent published successfully for orderId={}, partition={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
