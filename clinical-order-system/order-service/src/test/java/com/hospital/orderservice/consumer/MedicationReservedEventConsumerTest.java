package com.hospital.orderservice.consumer;

import com.hospital.orderservice.entity.Order;
import com.hospital.orderservice.entity.OrderStatus;
import com.hospital.orderservice.entity.OrderType;
import com.hospital.orderservice.entity.ProcessedEvent;
import com.hospital.orderservice.event.MedicationReservedEvent;
import com.hospital.orderservice.repository.OrderRepository;
import com.hospital.orderservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicationReservedEventConsumerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private MedicationReservedEventConsumer consumer;

    private UUID orderId;
    private String eventId;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        orderId  = UUID.randomUUID();
        eventId  = UUID.randomUUID().toString();
        pendingOrder = Order.builder()
                .orderId(orderId)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .orderType(OrderType.MEDICATION)
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void consume_RESERVED_shouldConfirmOrder() {
        MedicationReservedEvent event = MedicationReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .status("RESERVED")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void consume_FAILED_shouldRejectOrderWithReason() {
        MedicationReservedEvent event = MedicationReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .status("FAILED")
                .failureReason("Out of stock")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.consume(event);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(captor.getValue().getRejectionReason()).isEqualTo("Out of stock");
    }

    @Test
    void consume_duplicateEvent_shouldSkipProcessing() {
        MedicationReservedEvent event = MedicationReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .status("RESERVED")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        consumer.consume(event);

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }
}
