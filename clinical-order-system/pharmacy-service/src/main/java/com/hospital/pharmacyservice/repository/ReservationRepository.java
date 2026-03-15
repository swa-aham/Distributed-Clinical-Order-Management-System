package com.hospital.pharmacyservice.repository;

import com.hospital.pharmacyservice.entity.Reservation;
import com.hospital.pharmacyservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByOrderId(UUID orderId);
    List<Reservation> findByStatus(ReservationStatus status);
    boolean existsByOrderId(UUID orderId);
}
