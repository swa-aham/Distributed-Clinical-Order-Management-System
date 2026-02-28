package com.example.orderservice.event;

import com.example.orderservice.entity.OrderType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalOrderCreatedEvent {

    private UUID eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private OrderPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderPayload {
        private UUID orderId;
        private UUID patientId;
        private OrderType orderType;
    }
}