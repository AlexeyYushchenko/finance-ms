package ru.utlc.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.model.PaymentStatus;

public interface PaymentStatusRepository extends R2dbcRepository<PaymentStatus, Integer> {
}
