package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.RateUpdateLog;

import java.time.LocalDate;

public interface RateUpdateLogRepository extends ReactiveCrudRepository<RateUpdateLog, Integer> {

    @Query("SELECT EXISTS(SELECT 1 FROM rate_update_log WHERE update_date = :date AND status = TRUE)")
    Mono<Boolean> existsByUpdateDate(LocalDate date);

    @Query("INSERT INTO rate_update_log (update_date, status) VALUES (:date, TRUE)")
    Mono<Void> saveLogForToday(LocalDate date);
}
