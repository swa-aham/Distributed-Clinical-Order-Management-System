package com.hospital.pharmacyservice.repository;

import com.hospital.pharmacyservice.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    // Pessimistic lock prevents race conditions when multiple orders come in simultaneously
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medication m WHERE m.medicationId = :id")
    Optional<Medication> findByIdWithLock(UUID id);

    Optional<Medication> findByMedicationNameIgnoreCase(String name);
}
