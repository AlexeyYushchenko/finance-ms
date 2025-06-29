package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.model.PaymentStatus;

public interface PaymentStatusRepository extends R2dbcRepository<PaymentStatus, Integer> {
}
