package com.example.orderservice.dto;

import com.example.orderservice.entity.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderRequest {

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID doctorId;

    @NotNull
    private OrderType orderType;
}