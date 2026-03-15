package com.hospital.orderservice.service;

import com.hospital.orderservice.dto.CreateOrderRequest;
import com.hospital.orderservice.dto.OrderResponse;
import com.hospital.orderservice.entity.Order;
import com.hospital.orderservice.entity.OrderStatus;
import com.hospital.orderservice.entity.OrderType;
import com.hospital.orderservice.exception.OrderNotFoundException;
import com.hospital.orderservice.producer.OrderEventProducer;
import com.hospital.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private UUID patientId;
    private UUID doctorId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId  = UUID.randomUUID();
        orderId   = UUID.randomUUID();
    }

    @Test
    void createOrder_shouldPersistAndPublishEvent() {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .orderType("MEDICATION")
                .notes("Test order")
                .build();

        Order savedOrder = Order.builder()
                .orderId(orderId)
                .patientId(patientId)
                .doctorId(doctorId)
                .orderType(OrderType.MEDICATION)
                .status(OrderStatus.PENDING)
                .notes("Test order")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getOrderType()).isEqualTo("MEDICATION");
        assertThat(response.getPatientId()).isEqualTo(patientId);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).publishOrderCreatedEvent(any());
    }

    @Test
    void createOrder_withInvalidOrderType_shouldThrowException() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .orderType("INVALID_TYPE")
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid orderType");

        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).publishOrderCreatedEvent(any());
    }

    @Test
    void getOrderById_whenExists_shouldReturnOrder() {
        Order order = Order.builder()
                .orderId(orderId)
                .patientId(patientId)
                .doctorId(doctorId)
                .orderType(OrderType.LAB_TEST)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void getOrderById_whenNotFound_shouldThrowOrderNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void cancelOrder_whenPending_shouldSetCancelled() {
        Order order = Order.builder()
                .orderId(orderId)
                .patientId(patientId)
                .doctorId(doctorId)
                .orderType(OrderType.MEDICATION)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_whenNotPending_shouldThrowException() {
        Order order = Order.builder()
                .orderId(orderId)
                .patientId(patientId)
                .doctorId(doctorId)
                .orderType(OrderType.MEDICATION)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PENDING orders");
    }
}
