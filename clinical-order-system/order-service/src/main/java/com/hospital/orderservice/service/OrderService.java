package com.hospital.orderservice.service;

import com.hospital.orderservice.dto.CreateOrderRequest;
import com.hospital.orderservice.dto.OrderResponse;
import com.hospital.orderservice.entity.Order;
import com.hospital.orderservice.entity.OrderStatus;
import com.hospital.orderservice.entity.OrderType;
import com.hospital.orderservice.event.ClinicalOrderCreatedEvent;
import com.hospital.orderservice.exception.OrderNotFoundException;
import com.hospital.orderservice.producer.OrderEventProducer;
import com.hospital.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        OrderType orderType;
        try {
            orderType = OrderType.valueOf(request.getOrderType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderType: " + request.getOrderType()
                    + ". Valid values: MEDICATION, LAB_TEST, IMAGING, PROCEDURE, CONSULTATION");
        }

        Order order = Order.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .orderType(orderType)
                .status(OrderStatus.PENDING)
                .notes(request.getNotes())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created: orderId={}, type={}", saved.getOrderId(), saved.getOrderType());

        ClinicalOrderCreatedEvent event = ClinicalOrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(saved.getOrderId())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .orderType(saved.getOrderType().name())
                .notes(saved.getNotes())
                .timestamp(LocalDateTime.now())
                .build();

        orderEventProducer.publishOrderCreatedEvent(event);

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByPatient(UUID patientId) {
        return orderRepository.findByPatientId(patientId)
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled: orderId={}", saved.getOrderId());
        return OrderResponse.from(saved);
    }
}
