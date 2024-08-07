package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.model.InvoiceStatus;

public interface InvoiceStatusRepository extends R2dbcRepository<InvoiceStatus, Integer> {
}

