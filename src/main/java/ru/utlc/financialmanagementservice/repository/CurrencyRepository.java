package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.utlc.financialmanagementservice.model.Currency;

public interface CurrencyRepository extends JpaRepository<Currency, Integer> {

}