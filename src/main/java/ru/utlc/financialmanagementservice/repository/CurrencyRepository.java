package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.Currency;

public interface CurrencyRepository extends R2dbcRepository<Currency, Integer> {}