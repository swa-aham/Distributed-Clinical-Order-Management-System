package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.ClinicalOrderCreatedEvent;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTemplate<String, ClinicalOrderCreatedEvent> kafkaTemplate;

    @Transactional
    public UUID createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .orderType(request.getOrderType())
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        publishEvent(order);

        return order.getId();
    }

    private void publishEvent(Order order) {

        ClinicalOrderCreatedEvent event =
                ClinicalOrderCreatedEvent.builder()
                        .eventId(UUID.randomUUID())
                        .eventType("CLINICAL_ORDER_CREATED")
                        .timestamp(LocalDateTime.now())
                        .payload(
                                ClinicalOrderCreatedEvent.OrderPayload.builder()
                                        .orderId(order.getId())
                                        .patientId(order.getPatientId())
                                        .orderType(order.getOrderType())
                                        .build()
                        )
                        .build();

        try {
            kafkaTemplate.send("clinical-order-created", order.getId().toString(), event);
//            kafkaTemplate.send(topic, message);
        } catch (Exception e) {
//            log.error("Failed to publish to Kafka: {}", e.getMessage());
            System.out.println("Failed to publish to Kafka: " + e.getMessage());
            e.printStackTrace();
            // Decide if you want to fail the order or just log the error
        }

    }
}