package com.hospital.pharmacyservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ClinicalOrderCreatedEvent {

    private String eventId;
    private UUID orderId;
    private UUID patientId;
    private UUID doctorId;
    private String orderType;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
