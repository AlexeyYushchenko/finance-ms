package ru.utlc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.dto.partnerbalance.PartnerBalanceReportDto;
import ru.utlc.dto.partnerbalance.PartnerBalanceRowDto;
import ru.utlc.dto.currency.CurrencyReadDto;
import ru.utlc.model.InvoiceAggregate;
import ru.utlc.model.PaymentLeftoverAggregate;
import ru.utlc.repository.InvoiceRepository;
import ru.utlc.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerBalanceService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    private static final int RUB_CURRENCY_ID = 1;

    /**
     * Build a balance report for the given partner, at the given date.
     */
    public Mono<PartnerBalanceReportDto> getPartnerBalance(Long partnerId, LocalDate reportDate) {

        // 1) Payment leftover
        Mono<List<PaymentLeftoverAggregate>> leftoverMono =
                paymentRepository.sumLeftoverByPartner(partnerId).collectList();

        // 2) Invoice aggregator
        Mono<List<InvoiceAggregate>> invoiceMono =
                invoiceRepository.sumInvoicesByPartner(partnerId).collectList();

        // 3) Combine them, then build the final rows
        return Mono.zip(leftoverMono, invoiceMono)
                .flatMap(tuple -> {
                    List<PaymentLeftoverAggregate> leftoverAggs = tuple.getT1();
                    List<InvoiceAggregate> invoiceAggs = tuple.getT2();
                    return buildRows(leftoverAggs, invoiceAggs, reportDate)
                            .collectList(); // gather all row flux
                })
                .map(rows -> {
                    // 4) Sum up leftoverRub & outstandingRub across all rows
                    BigDecimal totalLeftoverRub = rows.stream()
                            .map(PartnerBalanceRowDto::leftoverRub)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalOutstandingRub = rows.stream()
                            .map(PartnerBalanceRowDto::outstandingRub)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new PartnerBalanceReportDto(
                            partnerId,
                            reportDate,
                            rows,
                            totalLeftoverRub,
                            totalOutstandingRub
                    );
                });
    }

    /**
     * Merges Payment leftover & Invoice aggregator data, grouping by currency,
     * then converts leftover & outstanding to rub.
     */
    private Flux<PartnerBalanceRowDto> buildRows(
            List<PaymentLeftoverAggregate> leftoverAggs,
            List<InvoiceAggregate> invoiceAggs,
            LocalDate reportDate
    ) {
        // Convert them to maps for easy lookup
        Map<Integer, BigDecimal> leftoverMap = leftoverAggs.stream()
                .collect(Collectors.toMap(
                        PaymentLeftoverAggregate::currencyId,
                        PaymentLeftoverAggregate::leftoverSum
                ));

        Map<Integer, InvoiceAggregate> invoiceMap = invoiceAggs.stream()
                .collect(Collectors.toMap(
                        InvoiceAggregate::currencyId,
                        agg -> agg
                ));

        // Union set of currency IDs
        Set<Integer> allCurrencies = new HashSet<>(leftoverMap.keySet());
        allCurrencies.addAll(invoiceMap.keySet());

        // We'll fetch all currency codes in memory => ID->"RUB"/"USD" etc.
        return currencyService.findAll()
                .collectMap(CurrencyReadDto::id, CurrencyReadDto::code)
                .flatMapMany(codeMap -> Flux.fromIterable(allCurrencies)
                        .flatMap(currId -> {
                            // Payment leftover
                            BigDecimal leftover = leftoverMap.getOrDefault(currId, BigDecimal.ZERO);

                            // Invoice aggregator
                            InvoiceAggregate inv = invoiceMap.get(currId);
                            BigDecimal unpaid = (inv != null) ? inv.totalUnpaid() : BigDecimal.ZERO;
                            BigDecimal partial = (inv != null) ? inv.partiallyPaid() : BigDecimal.ZERO;
                            BigDecimal outstanding = (inv != null) ? inv.outstanding() : BigDecimal.ZERO;

                            String code = codeMap.getOrDefault(currId, "???");

                            // We'll convert leftover->rub, outstanding->rub, then build row
                            return convertToRub(currId, leftover, reportDate)
                                    .zipWith(convertToRub(currId, outstanding, reportDate))
                                    .map(tuple -> {
                                        BigDecimal leftoverRub = tuple.getT1();
                                        BigDecimal outRub = tuple.getT2();

                                        return new PartnerBalanceRowDto(
                                                currId,
                                                code,
                                                leftover,
                                                unpaid,
                                                partial,
                                                outstanding,
                                                leftoverRub,
                                                outRub
                                        );
                                    });
                        })
                );
    }

    /**
     * Convert the given amount from {currencyId} to RUB at {reportDate}.
     * If it's already RUB, just return the same.
     */
    private Mono<BigDecimal> convertToRub(Integer currencyId, BigDecimal amount, LocalDate reportDate) {
        if (currencyId.equals(RUB_CURRENCY_ID)) {
            return Mono.just(amount);
        }
        return exchangeRateService.getExchangeRate(currencyId, RUB_CURRENCY_ID, reportDate)
                .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }
}
