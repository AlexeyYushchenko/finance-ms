package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.model.ServiceType;

public interface ServiceTypeRepository extends R2dbcRepository<ServiceType, Integer> {}
