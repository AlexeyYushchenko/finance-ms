package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.ValidationException;
import ru.utlc.financialmanagementservice.integration.PartnerService;
import ru.utlc.financialmanagementservice.model.InvoiceDirection;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import static ru.utlc.financialmanagementservice.constants.RefTypeConstants.REFTYPE_ALLOCATION;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА.  All rights reserved.
 * Licensed under Proprietary License.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final ExchangeRateService exchangeRateService;
    private final TransactionLedgerService ledgerService;
    private final PartnerService partnerService;

    private final ReactiveTransactionManager txManager;

    private TransactionalOperator operator() {
        return TransactionalOperator.create(txManager);
    }

    /**
     * Internal ref-type for FX legs (unchanged).
     */
    private static final int REFTYPE_CONVERSION = 4;

    /* ――――――――――――――――――――――――――――――――――――― FINDERS ――――――――――――――――――――――――――――――――― */
    public Flux<LedgerEntryReadDto> findByClientId(Long clientId) {
        return ledgerService.findAllocationsByClientId(clientId);
    }

    public Flux<LedgerEntryReadDto> findByInvoiceId(Long invoiceId) {
        return ledgerService.findAllocationsByInvoiceId(invoiceId);
    }

    public Flux<LedgerEntryReadDto> findByPaymentId(Long paymentId) {
        return ledgerService.findAllocationsByPaymentId(paymentId);
    }

    /* ――――――――――――――――――――――――――――――――――――― ALLOCATE ――――――――――――――――――――――――――――――――― */
    public Mono<Void> allocatePaymentToInvoice(Long paymentId, Long invoiceId, BigDecimal amount) {
        return Mono.zip(paymentService.findById(paymentId),
                        invoiceService.findById(invoiceId))
                .flatMap(t -> partnerService.findById(t.getT1().partnerId())
                        .then(partnerService.findById(t.getT2().partnerId()))
                        .then(doAllocate(t.getT1(), t.getT2(), amount)))
                .as(operator()::transactional);
    }

    /* ――――――――――――――――――――――――――――――――――――― DEALLOCATE ――――――――――――――――――――――――――――――― */
    public Mono<Void> deallocatePaymentFromInvoice(Long paymentId, Long invoiceId, BigDecimal amount) {
        return Mono.zip(paymentService.findById(paymentId),
                        invoiceService.findById(invoiceId))
                .flatMap(t -> partnerService.findById(t.getT1().partnerId())
                        .then(partnerService.findById(t.getT2().partnerId()))
                        .then(doDeallocate(t.getT1(), t.getT2(), amount)))
                .as(operator()::transactional);
    }

    /* ───────────────────────────── INTERNAL helpers ───────────────────────────── */

    /**
     * +1 for AR (you receive money), -1 for AP (you owe money).
     */
    private int signFor(InvoiceReadDto inv) {
        return inv.direction() == InvoiceDirection.PAYABLE ? -1 : 1;
    }

    private Mono<Void> doAllocate(PaymentReadDto pay, InvoiceReadDto inv, BigDecimal paySideAmt) {
        checkPaymentInvoiceSameClient(pay, inv);
        checkMinAmount(paySideAmt);

        Mono<BigDecimal> invAmtMono = convertIfNeeded(pay, inv, paySideAmt);

        return invAmtMono.flatMap(invAmt -> {
            int s = signFor(inv);
            BigDecimal signedInvAmt = invAmt.multiply(BigDecimal.valueOf(s));

            /* ▸ optimistic-locking retry (3 × 50 ms) */
            Mono<Void> payUpdate = paymentService.allocateFromPayment(pay.id(), paySideAmt)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(50)));

            Mono<Void> invUpdate = invoiceService.addToPaidAmount(inv.id(), invAmt)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(50)));

            Mono<Void> ledger;
            if (pay.currencyId().equals(inv.currencyId()))
                ledger = sameCurrencyAllocation(pay, inv, signedInvAmt);
            else
                ledger = crossCurrencyAllocation(pay, inv, paySideAmt, signedInvAmt);

            return payUpdate.then(invUpdate).then(ledger);
        });
    }

    private Mono<Void> doDeallocate(PaymentReadDto pay, InvoiceReadDto inv, BigDecimal paySideAmt) {
        checkPaymentInvoiceSameClient(pay, inv);
        checkMinAmount(paySideAmt);

        Mono<BigDecimal> alreadyAllocated = ledgerService
                .findAllocationsForPaymentInvoice(pay.id(), inv.id())
                .map(LedgerEntryReadDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Mono<BigDecimal> invAmtMono = convertIfNeeded(pay, inv, paySideAmt);

        return alreadyAllocated.zipWith(invAmtMono)
                .flatMap(t -> {
                    BigDecimal allocated = t.getT1();
                    BigDecimal invAmt = t.getT2();

                    /* we compare absolute values because AP rows are negative */
                    if (allocated.abs().compareTo(invAmt) < 0)
                        return Mono.error(new ValidationException("error.allocation.noExistingAllocationForPaymentInvoice"));

                    int s = signFor(inv);
                    BigDecimal signedInvAmt = invAmt.multiply(BigDecimal.valueOf(s));

                    Mono<Void> payUpdate = paymentService.deallocateToPayment(pay.id(), paySideAmt)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(50)));
                    Mono<Void> invUpdate = invoiceService.subtractFromPaidAmount(inv.id(), invAmt)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(50)));

                    LocalDate d = inv.issueDate() != null ? inv.issueDate() : pay.paymentDate();
                    Mono<Void> reversalRow = ledgerService.writeAllocationRow(new LedgerEntryCreateUpdateDto(
                            pay.partnerId(),
                            inv.currencyId(),
                            signedInvAmt.negate(),       // opposite of the original sign
                            REFTYPE_ALLOCATION,
                            inv.id(),
                            pay.id(),
                            d));

                    return payUpdate.then(invUpdate).then(reversalRow);
                });
    }

    /* ~~~~~~~~~ ledger writers ~~~~~~~~~ */

    private Mono<Void> sameCurrencyAllocation(PaymentReadDto pay,
                                              InvoiceReadDto inv,
                                              BigDecimal signedInvAmt) {
        LocalDate d = inv.issueDate() != null ? inv.issueDate() : pay.paymentDate();
        return ledgerService.writeAllocationRow(new LedgerEntryCreateUpdateDto(
                pay.partnerId(), inv.currencyId(), signedInvAmt,
                REFTYPE_ALLOCATION, inv.id(), pay.id(), d));
    }

    private Mono<Void> crossCurrencyAllocation(PaymentReadDto pay,
                                               InvoiceReadDto inv,
                                               BigDecimal paySideAmt,
                                               BigDecimal signedInvAmt) {
        LocalDate d = inv.issueDate() != null ? inv.issueDate() : pay.paymentDate();

        /* 1  FX “from” leg  →  −USD */
        LedgerEntryCreateUpdateDto fromRow = new LedgerEntryCreateUpdateDto(
                pay.partnerId(), pay.currencyId(), paySideAmt.negate(),
                REFTYPE_CONVERSION, null, pay.id(), d);

        /* 2  FX “to” leg    →  +RUB  (or whatever) */
        LedgerEntryCreateUpdateDto toRow = new LedgerEntryCreateUpdateDto(
                inv.partnerId(), inv.currencyId(), signedInvAmt,   // signedInvAmt is already ±
                REFTYPE_CONVERSION, null, pay.id(), d);

        /* 3  Allocation row */
        LedgerEntryCreateUpdateDto allocRow = new LedgerEntryCreateUpdateDto(
                pay.partnerId(), inv.currencyId(), signedInvAmt,
                REFTYPE_ALLOCATION, inv.id(), pay.id(), d);

        return ledgerService.writeTwoRowsForConversion(fromRow, toRow)
                .then(ledgerService.writeAllocationRow(allocRow));
    }

    /* ~~~~~~~~~ utilities ~~~~~~~~~ */

    private Mono<BigDecimal> convertIfNeeded(PaymentReadDto pay, InvoiceReadDto inv, BigDecimal payAmt) {
        if (pay.currencyId().equals(inv.currencyId()))
            return Mono.just(payAmt);

        LocalDate d = inv.issueDate() != null ? inv.issueDate() : pay.paymentDate();
        return exchangeRateService.convertAmount(pay.currencyId(), inv.currencyId(), payAmt, d);
    }

    private void checkPaymentInvoiceSameClient(PaymentReadDto pay, InvoiceReadDto inv) {
        if (!pay.partnerId().equals(inv.partnerId()))
            throw new ValidationException("error.paymentInvoice.paymentAndInvoiceClientMismatch");
    }

    private void checkMinAmount(BigDecimal amt) {
        if (amt.compareTo(BigDecimal.valueOf(0.01)) < 0)
            throw new ValidationException("validation.paymentAllocation.allocatedAmount.min");
    }
}
