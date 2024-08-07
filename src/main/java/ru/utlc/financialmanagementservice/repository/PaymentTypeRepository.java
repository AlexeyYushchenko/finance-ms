package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.model.PaymentType;

public interface PaymentTypeRepository extends R2dbcRepository<PaymentType, Integer> {
}