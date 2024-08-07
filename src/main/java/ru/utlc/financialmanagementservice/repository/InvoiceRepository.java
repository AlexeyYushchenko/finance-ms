package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.model.Invoice;

public interface InvoiceRepository extends R2dbcRepository<Invoice, Long> {
}

