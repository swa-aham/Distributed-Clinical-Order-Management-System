package com.hospital.orderservice.repository;

import com.hospital.orderservice.entity.Order;
import com.hospital.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByPatientId(UUID patientId);
    List<Order> findByDoctorId(UUID doctorId);
    List<Order> findByStatus(OrderStatus status);
}
