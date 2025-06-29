package ru.utlc.financialmanagementservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.ExchangeRate;

/**
 * Implementation of custom repository logic for ExchangeRate.
 */
@Repository
@RequiredArgsConstructor
public class ExchangeRateRepositoryCustomImpl implements ExchangeRateRepositoryCustom {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> upsertExchangeRate(ExchangeRate exchangeRate) {
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
                DO UPDATE SET official_rate = exchange_rate.official_rate
                """;

        return databaseClient.sql(sql)
                .bind("fromId", exchangeRate.getCurrencyFromId())
                .bind("toId", exchangeRate.getCurrencyToId())
                .bind("rateDate", exchangeRate.getRateDate())
                .bind("officialRate", exchangeRate.getOfficialRate())
                .bind("standardRate", exchangeRate.getStandardRate())
                .bind("premiumClientRate", exchangeRate.getPremiumClientRate())
                .fetch().rowsUpdated()
                .then();
    }
}
