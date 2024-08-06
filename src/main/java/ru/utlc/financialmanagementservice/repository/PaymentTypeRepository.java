package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.utlc.financialmanagementservice.model.PaymentType;

public interface PaymentTypeRepository extends JpaRepository<PaymentType, Integer> {
}