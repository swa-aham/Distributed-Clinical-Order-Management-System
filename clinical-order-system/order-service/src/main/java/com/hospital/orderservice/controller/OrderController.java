package com.hospital.orderservice.controller;

import com.hospital.orderservice.dto.CreateOrderRequest;
import com.hospital.orderservice.dto.OrderResponse;
import com.hospital.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/orders - patientId={}, orderType={}", request.getPatientId(), request.getOrderType());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(orderService.getOrdersByPatient(patientId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        log.info("PATCH /api/orders/{}/cancel", orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }
}
