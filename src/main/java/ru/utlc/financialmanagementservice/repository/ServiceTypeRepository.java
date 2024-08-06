package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.utlc.financialmanagementservice.model.ServiceType;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Integer> {
}
