package com.hospital.orderservice.dto;

import com.hospital.orderservice.entity.Order;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID orderId;
    private UUID patientId;
    private UUID doctorId;
    private String orderType;
    private String status;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .patientId(order.getPatientId())
                .doctorId(order.getDoctorId())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .notes(order.getNotes())
                .rejectionReason(order.getRejectionReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
