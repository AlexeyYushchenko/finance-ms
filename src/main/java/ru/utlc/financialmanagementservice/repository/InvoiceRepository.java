package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.utlc.financialmanagementservice.model.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
//public interface InvoiceRepository extends R2dbcRepository<Invoice, Long> {
//}

