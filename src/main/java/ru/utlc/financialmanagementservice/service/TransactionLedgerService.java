package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.AllocationNotFoundException;
import ru.utlc.financialmanagementservice.mapper.LedgerEntryMapper;
import ru.utlc.financialmanagementservice.model.LedgerEntry;
import ru.utlc.financialmanagementservice.repository.LedgerEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static ru.utlc.financialmanagementservice.constants.RefTypeConstants.*;

/**
 * A low-level ledger-writing service with convenience methods for Payment/Invoice creation,
 * adjustment, and reversal, plus cross-currency support if needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryMapper ledgerEntryMapper;

    private final ExchangeRateService exchangeRateService;
    private final CurrencyRateService currencyRateService;

    private final ReactiveTransactionManager txManager;
    private static final int REFTYPE_ALLOCATION = 3;

    private TransactionalOperator operator() {
        return TransactionalOperator.create(txManager);
    }

    /*
     * ------------------------------------------------
     * Basic CRUD
     * ------------------------------------------------
     */

    public Flux<LedgerEntryReadDto> findAll() {
        return ledgerEntryRepository.findAll()
                .map(ledgerEntryMapper::toReadDto);
    }

    public Mono<LedgerEntryReadDto> findById(Long ledgerId) {
        return ledgerEntryRepository.findById(ledgerId)
                .map(ledgerEntryMapper::toReadDto);
    }


    public Flux<LedgerEntryReadDto> findAllocationsByClientId(Long clientId) {
        return ledgerEntryRepository.findAllocationsByPartnerId(clientId)
                .switchIfEmpty(Flux.error(new AllocationNotFoundException("error.paymentInvoice.client.notFound", clientId)))
                .map(ledgerEntryMapper::toReadDto);
    }

    public Flux<LedgerEntryReadDto> findAllocationsByInvoiceId(Long invoiceId) {
        return ledgerEntryRepository.findAllocationsByInvoiceId(invoiceId)
                .switchIfEmpty(Flux.error(new AllocationNotFoundException("error.paymentInvoice.invoice.notFound", invoiceId)))
                .map(ledgerEntryMapper::toReadDto);
    }

    public Flux<LedgerEntryReadDto> findAllocationsByPaymentId(Long paymentId) {
        return ledgerEntryRepository.findAllocationsByPaymentId(paymentId)
                .switchIfEmpty(Flux.error(new AllocationNotFoundException("error.paymentInvoice.payment.notFound", paymentId)))
                .map(ledgerEntryMapper::toReadDto);
    }

    /**
     * Generic single-row method
     */
    public Mono<LedgerEntryReadDto> createLedgerEntry(LedgerEntryCreateUpdateDto dto) {
        LedgerEntry entity = ledgerEntryMapper.toEntity(dto);
        return computeBaseAmount(entity.getCurrencyId(), entity.getAmount(), entity.getTransactionDate())
                .flatMap(baseAmt -> {
                    entity.setBaseAmount(baseAmt);
                    return ledgerEntryRepository.save(entity);
                })
                .map(ledgerEntryMapper::toReadDto)
                .as(operator()::transactional);
    }

    /*
     * ------------------------------------------------
     * Payment convenience methods
     * ------------------------------------------------
     */

    public Mono<Void> createPaymentLedgerRow(PaymentReadDto paymentReadDto) {
        return createLedgerEntry(LedgerEntryCreateUpdateDto.forPayment(paymentReadDto)).then();
    }

    public Mono<Void> createPaymentAdjustmentRow(PaymentReadDto paymentReadDto, BigDecimal difference) {
        if (difference.compareTo(BigDecimal.ZERO) == 0) return Mono.empty();
        return createLedgerEntry(LedgerEntryCreateUpdateDto.forPaymentAdjustment(paymentReadDto, difference)).then();
    }

    public Mono<Void> createPaymentReversalRow(PaymentReadDto paymentReadDto) {
        return createLedgerEntry(LedgerEntryCreateUpdateDto.forPaymentReversal(paymentReadDto)).then();
    }

    /*
     * ------------------------------------------------
     * Invoice convenience methods
     * ------------------------------------------------
     */

    public Mono<Void> createInvoiceLedgerRow(InvoiceReadDto invoiceReadDto) {
        return createLedgerEntry(LedgerEntryCreateUpdateDto.forInvoice(invoiceReadDto)).then();
    }

    public Mono<Void> createInvoiceAdjustmentRow(InvoiceReadDto invoiceReadDto, BigDecimal difference) {
        if (difference.compareTo(BigDecimal.ZERO) == 0) return Mono.empty();
        return createLedgerEntry(LedgerEntryCreateUpdateDto.forInvoiceAdjustment(invoiceReadDto, difference)).then();
    }

    public Mono<Void> createInvoiceReversalRow(InvoiceReadDto invoiceReadDto) {
        return createLedgerEntry(LedgerEntryCreateUpdateDto.forInvoiceReversal(invoiceReadDto)).then();
    }

    /*
     * ------------------------------------------------
     * Cross-currency convenience
     * ------------------------------------------------
     */

    public Mono<Void> writeTwoRowsForConversion(
            LedgerEntryCreateUpdateDto fromDto,
            LedgerEntryCreateUpdateDto toDto
    ) {
        return createLedgerEntry(fromDto)
                .flatMap(__ -> createLedgerEntry(toDto))
                .then();
    }

    /*
     * ------------------------------------------------
     * Helper: computeBaseAmount
     * ------------------------------------------------
     */

    private Mono<BigDecimal> computeBaseAmount(Integer currencyId, BigDecimal amount, LocalDate date) {
        if (currencyId.equals(RUB_CURRENCY_ID)) {
            return Mono.just(amount);
        }
        return exchangeRateService.getExchangeRate(currencyId, RUB_CURRENCY_ID, date)
                .switchIfEmpty(
                        currencyRateService.fetchAndSaveRates(date)
                                .then(exchangeRateService.getExchangeRate(currencyId, RUB_CURRENCY_ID, date))
                )
                .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }

    public Mono<Void> writeAllocationRow(Long partnerId,
                                         Integer currencyId,
                                         BigDecimal amount,
                                         Long paymentId,
                                         Long invoiceId,
                                         LocalDate date) {
        // reference_type_id=3 => ALLOCATION
        LedgerEntryCreateUpdateDto dto = new LedgerEntryCreateUpdateDto(
                partnerId, currencyId, amount, REFTYPE_ALLOCATION, paymentId, invoiceId, date
        );
        return createLedgerEntry(dto).then();
    }
}
