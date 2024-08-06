package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.utlc.financialmanagementservice.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}