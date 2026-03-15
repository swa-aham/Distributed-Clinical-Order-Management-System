package com.hospital.orderservice.consumer;

import com.hospital.orderservice.entity.OrderStatus;
import com.hospital.orderservice.entity.ProcessedEvent;
import com.hospital.orderservice.event.MedicationReservedEvent;
import com.hospital.orderservice.repository.OrderRepository;
import com.hospital.orderservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicationReservedEventConsumer {

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "medication-reserved",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(MedicationReservedEvent event) {
        log.info("Received MedicationReservedEvent: orderId={}, status={}, eventId={}",
                event.getOrderId(), event.getStatus(), event.getEventId());

        // Idempotency check
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate MedicationReservedEvent detected, skipping. eventId={}", event.getEventId());
            return;
        }

        orderRepository.findById(event.getOrderId()).ifPresentOrElse(order -> {
            if ("RESERVED".equalsIgnoreCase(event.getStatus())) {
                order.setStatus(OrderStatus.CONFIRMED);
                log.info("Order confirmed: orderId={}", order.getOrderId());
            } else {
                order.setStatus(OrderStatus.REJECTED);
                order.setRejectionReason(event.getFailureReason());
                log.warn("Order rejected: orderId={}, reason={}", order.getOrderId(), event.getFailureReason());
            }
            orderRepository.save(order);
        }, () -> log.error("Order not found for MedicationReservedEvent: orderId={}", event.getOrderId()));

        // Mark event as processed
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .processedAt(LocalDateTime.now())
                .eventType("MedicationReservedEvent")
                .build());
    }
}
