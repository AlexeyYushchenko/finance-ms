package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.utlc.financialmanagementservice.model.InvoiceStatus;

public interface InvoiceStatusRepository extends JpaRepository<InvoiceStatus, Integer> {
}

