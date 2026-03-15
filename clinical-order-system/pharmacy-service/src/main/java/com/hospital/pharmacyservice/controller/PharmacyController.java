package com.hospital.pharmacyservice.controller;

import com.hospital.pharmacyservice.entity.Medication;
import com.hospital.pharmacyservice.entity.Reservation;
import com.hospital.pharmacyservice.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @GetMapping("/medications")
    public ResponseEntity<List<Medication>> getMedications() {
        return ResponseEntity.ok(pharmacyService.getAllMedications());
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getReservations() {
        return ResponseEntity.ok(pharmacyService.getAllReservations());
    }

    @GetMapping("/reservations/order/{orderId}")
    public ResponseEntity<Reservation> getReservationByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(pharmacyService.getReservationByOrderId(orderId));
    }
}
