package com.hospital.pharmacyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "medication_id", updatable = false, nullable = false)
    private UUID medicationId;

    @Column(name = "medication_name", nullable = false, unique = true)
    private String medicationName;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "unit")
    private String unit;

    @Column(name = "description")
    private String description;
}
