package com.hospital.pharmacyservice.service;

import com.hospital.pharmacyservice.entity.Medication;
import com.hospital.pharmacyservice.entity.Reservation;
import com.hospital.pharmacyservice.entity.ReservationStatus;
import com.hospital.pharmacyservice.event.ClinicalOrderCreatedEvent;
import com.hospital.pharmacyservice.event.MedicationReservedEvent;
import com.hospital.pharmacyservice.producer.MedicationReservedProducer;
import com.hospital.pharmacyservice.repository.MedicationRepository;
import com.hospital.pharmacyservice.repository.ProcessedEventRepository;
import com.hospital.pharmacyservice.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock private MedicationRepository medicationRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private MedicationReservedProducer reservedProducer;

    @InjectMocks
    private PharmacyService pharmacyService;

    private UUID orderId;
    private String eventId;
    private Medication medication;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        eventId    = UUID.randomUUID().toString();
        medication = Medication.builder()
                .medicationId(UUID.randomUUID())
                .medicationName("Amoxicillin")
                .stock(10)
                .unit("tablets")
                .build();
    }

    private ClinicalOrderCreatedEvent buildEvent(String orderType) {
        return ClinicalOrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .orderType(orderType)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void processOrderEvent_medicationOrder_stockAvailable_shouldReserve() {
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(false);
        when(medicationRepository.findAll()).thenReturn(List.of(medication));
        when(medicationRepository.findByIdWithLock(medication.getMedicationId()))
                .thenReturn(Optional.of(medication));
        when(medicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pharmacyService.processOrderEvent(buildEvent("MEDICATION"));

        // Verify stock was decremented
        ArgumentCaptor<Medication> medCaptor = ArgumentCaptor.forClass(Medication.class);
        verify(medicationRepository).save(medCaptor.capture());
        assertThat(medCaptor.getValue().getStock()).isEqualTo(9);

        // Verify reservation created with RESERVED status
        ArgumentCaptor<Reservation> resCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(resCaptor.capture());
        assertThat(resCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);

        // Verify event published with RESERVED
        ArgumentCaptor<MedicationReservedEvent> eventCaptor =
                ArgumentCaptor.forClass(MedicationReservedEvent.class);
        verify(reservedProducer).publishReservationResult(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo("RESERVED");
    }

    @Test
    void processOrderEvent_medicationOrder_noStock_shouldFail() {
        Medication emptyStock = Medication.builder()
                .medicationId(UUID.randomUUID())
                .medicationName("Ibuprofen")
                .stock(0)
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(false);
        when(medicationRepository.findAll()).thenReturn(List.of(emptyStock));

        pharmacyService.processOrderEvent(buildEvent("MEDICATION"));

        ArgumentCaptor<MedicationReservedEvent> eventCaptor =
                ArgumentCaptor.forClass(MedicationReservedEvent.class);
        verify(reservedProducer).publishReservationResult(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(eventCaptor.getValue().getFailureReason()).contains("stock");
    }

    @Test
    void processOrderEvent_nonMedicationOrder_shouldSkipAndMarkProcessed() {
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);

        pharmacyService.processOrderEvent(buildEvent("LAB_TEST"));

        verify(medicationRepository, never()).findAll();
        verify(reservedProducer, never()).publishReservationResult(any());
        verify(processedEventRepository).save(any());
    }

    @Test
    void processOrderEvent_duplicateEvent_shouldSkipEntirely() {
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        pharmacyService.processOrderEvent(buildEvent("MEDICATION"));

        verify(medicationRepository, never()).findAll();
        verify(reservedProducer, never()).publishReservationResult(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void processOrderEvent_duplicateOrderId_shouldSkipReservation() {
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(true);

        pharmacyService.processOrderEvent(buildEvent("MEDICATION"));

        verify(medicationRepository, never()).findAll();
        verify(reservedProducer, never()).publishReservationResult(any());
    }
}
