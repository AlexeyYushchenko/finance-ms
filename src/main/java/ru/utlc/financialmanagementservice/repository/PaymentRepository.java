package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.model.Payment;

public interface PaymentRepository extends R2dbcRepository<Payment, Long> {
}