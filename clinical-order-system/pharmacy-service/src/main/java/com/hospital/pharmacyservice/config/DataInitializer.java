package com.hospital.pharmacyservice.config;

import com.hospital.pharmacyservice.entity.Medication;
import com.hospital.pharmacyservice.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MedicationRepository medicationRepository;

    @Override
    public void run(String... args) {
        if (medicationRepository.count() == 0) {
            log.info("Seeding initial medication data...");
            List<Medication> medications = List.of(
                    Medication.builder().medicationName("Amoxicillin").stock(100).unit("tablets").description("Antibiotic").build(),
                    Medication.builder().medicationName("Ibuprofen").stock(200).unit("tablets").description("NSAID pain reliever").build(),
                    Medication.builder().medicationName("Metformin").stock(150).unit("tablets").description("Diabetes medication").build(),
                    Medication.builder().medicationName("Lisinopril").stock(80).unit("tablets").description("ACE inhibitor").build(),
                    Medication.builder().medicationName("Atorvastatin").stock(120).unit("tablets").description("Cholesterol medication").build()
            );
            medicationRepository.saveAll(medications);
            log.info("Seeded {} medications", medications.size());
        } else {
            log.info("Medication data already exists, skipping seed");
        }
    }
}
