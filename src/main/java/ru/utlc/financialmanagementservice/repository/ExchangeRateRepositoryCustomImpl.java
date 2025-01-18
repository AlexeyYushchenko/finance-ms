package ru.utlc.financialmanagementservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Implementation of custom repository logic for ExchangeRate.
 */
@Repository
@RequiredArgsConstructor
public class ExchangeRateRepositoryCustomImpl implements ExchangeRateRepositoryCustom {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<ExchangeRate> upsertExchangeRate(ExchangeRate exchangeRate) {
        String sql = """
            INSERT INTO exchange_rate (
                currency_from_id,
                currency_to_id,
                rate_date,
                official_rate,
                standard_rate,
                premium_client_rate,
                created_at,
                modified_at
            ) VALUES (
                :fromId, :toId, :rateDate,
                :officialRate, :standardRate, :premiumClientRate,
                NOW(), NOW()
            )
            ON CONFLICT (currency_from_id, currency_to_id, rate_date)
            DO NOTHING
            RETURNING *
            """;

        return databaseClient.sql(sql)
                .bind("fromId", exchangeRate.getCurrencyFromId())
                .bind("toId", exchangeRate.getCurrencyToId())
                .bind("rateDate", exchangeRate.getRateDate())
                .bind("officialRate", exchangeRate.getOfficialRate())
                .bind("standardRate", exchangeRate.getStandardRate())
                .bind("premiumClientRate", exchangeRate.getPremiumClientRate())
                .map((row, meta) -> mapRowToExchangeRate(row))
                .one();
    }

    private ExchangeRate mapRowToExchangeRate(io.r2dbc.spi.Row row) {
        ExchangeRate er = new ExchangeRate();
        er.setId(row.get("id", Long.class));
        er.setCurrencyFromId(row.get("currency_from_id", Integer.class));
        er.setCurrencyToId(row.get("currency_to_id", Integer.class));
        er.setRateDate(row.get("rate_date", LocalDate.class));
        er.setOfficialRate(row.get("official_rate", BigDecimal.class));
        er.setStandardRate(row.get("standard_rate", BigDecimal.class));
        er.setPremiumClientRate(row.get("premium_client_rate", BigDecimal.class));
        er.setCreatedAt(row.get("created_at", LocalDateTime.class));
        er.setModifiedAt(row.get("modified_at", LocalDateTime.class));
        er.setCreatedBy(row.get("created_by", String.class));
        er.setModifiedBy(row.get("modified_by", String.class));
        return er;
    }
}
