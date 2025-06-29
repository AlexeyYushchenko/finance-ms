package ru.utlc.financialmanagementservice.service;

//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.ReactiveTransactionManager;
//import org.springframework.transaction.reactive.TransactionalOperator;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.util.function.Tuple2;
//import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
//import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceCreateDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceReadDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceUpdateDto;
//import ru.utlc.financialmanagementservice.exception.AllocationNotFoundException;
//import ru.utlc.financialmanagementservice.exception.ExchangeRateRetrievalFailedException;
//import ru.utlc.financialmanagementservice.exception.ValidationException;
//import ru.utlc.financialmanagementservice.mapper.PaymentInvoiceMapper;
//import ru.utlc.financialmanagementservice.model.PaymentInvoice;
//import ru.utlc.financialmanagementservice.repository.PaymentInvoiceRepository;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//
///*
// * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
// * Licensed under Proprietary License.
// *
// * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
// * Date: 2024-08-19
// */
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class PaymentInvoiceService {
//
//    private final PaymentInvoiceRepository paymentInvoiceRepository;
//    private final PaymentInvoiceMapper paymentInvoiceMapper;
//    private final PaymentService paymentService;
//    private final InvoiceService invoiceService;
//    private final ExchangeRateService exchangeRateService;
//    private final ReactiveTransactionManager transactionManager;
//
//    private static final Integer RUB_CURRENCY_ID = 1;
//
//    private TransactionalOperator transactionalOperator() {
//        return TransactionalOperator.create(transactionManager);
//    }
//
//    /*
//     * -----------------------
//     *       PUBLIC API
//     * -----------------------
//     */
//
//    public Flux<PaymentInvoiceReadDto> findAll() {
//        return paymentInvoiceRepository.findAll()
//                .map(paymentInvoiceMapper::toDto);
//    }
//
//    public Mono<PaymentInvoiceReadDto> findById(Long allocationId) {
//        return paymentInvoiceRepository.findById(allocationId)
//                .switchIfEmpty(Mono.error(new AllocationNotFoundException(
//                        "error.paymentInvoice.allocationNotFound", allocationId
//                )))
//                .map(paymentInvoiceMapper::toDto);
//    }
//
//    public Flux<PaymentInvoiceReadDto> findAllByClientId(Integer partnerId) {
//        return paymentInvoiceRepository.findAllByClientId(partnerId)
//                .switchIfEmpty(Mono.error(new AllocationNotFoundException("error.paymentInvoice.client.notFound", partnerId)))
//                .map(paymentInvoiceMapper::toDto);
//    }
//
//    public Flux<PaymentInvoiceReadDto> findAllByPaymentId(Long paymentId) {
//        return paymentInvoiceRepository.findAllByPaymentId(paymentId)
//                .switchIfEmpty(Mono.error(new AllocationNotFoundException("error.paymentInvoice.payment.notFound", paymentId)))
//                .map(paymentInvoiceMapper::toDto);
//    }
//
//    public Flux<PaymentInvoiceReadDto> findAllByInvoiceId(Long invoiceId) {
//        return paymentInvoiceRepository.findAllByInvoiceId(invoiceId)
//                .switchIfEmpty(Mono.error(new AllocationNotFoundException("error.paymentInvoice.invoice.notFound", invoiceId)))
//                .map(paymentInvoiceMapper::toDto);
//    }
//
//    public Mono<PaymentInvoiceReadDto> allocatePaymentToInvoice(PaymentInvoiceCreateDto createDto) {
//
//        return Mono.just(paymentInvoiceMapper.toCreateUpdateDto(createDto))
//                .flatMap(this::enrichDtoForCreation)
//                .flatMap(enrichedDto -> validateAllocation(enrichedDto, null))
//                .flatMap(enrichedDto ->
//                        paymentService.allocateFromPayment(
//                                        enrichedDto.paymentId(),
//                                        enrichedDto.allocatedAmount())
//                                .thenReturn(enrichedDto))
//                .map(paymentInvoiceMapper::toEntity)
//                .flatMap(paymentInvoiceRepository::save)
//                .map(paymentInvoiceMapper::toDto)
//                .as(transactionalOperator()::transactional);
//    }
//
//    public Mono<PaymentInvoiceReadDto> updateAllocation(Long allocationId, PaymentInvoiceUpdateDto updateDto) {
//        // On update, we re-use the stored exchangeRate from DB
//        return paymentInvoiceRepository.findById(allocationId)
//                .switchIfEmpty(Mono.error(new AllocationNotFoundException(
//                        "error.paymentInvoice.allocationNotFound", allocationId
//                )))
//                .flatMap(existingAllocation -> processUpdate(existingAllocation, updateDto))
//                .as(transactionalOperator()::transactional);
//    }
//
//    public Mono<Void> deallocatePaymentFromInvoice(Long allocationId) {
//        return paymentInvoiceRepository.findById(allocationId)
//                .switchIfEmpty(Mono.error(new AllocationNotFoundException(
//                        "error.paymentInvoice.allocationNotFound", allocationId
//                )))
//                .flatMap(allocation -> {
//                    Long paymentId = allocation.getPaymentId();
//                    BigDecimal allocatedAmount = allocation.getAllocatedAmount();
//
//                    // Delegate the responsibility of updating the Payment to PaymentService
//                    return paymentService.deallocateToPayment(paymentId, allocatedAmount)
//                            .then(paymentInvoiceRepository.delete(allocation));
//                })
//                .as(transactionalOperator()::transactional);
//    }
//
//
//    /*
//     * -----------------------
//     *    INTERNAL METHODS
//     * -----------------------
//     */
//
//    /**
//     * The update flow: we only allow changing the allocated amount (and maybe other fields),
//     * but we do NOT fetch a new exchange rate. Instead, we re-use the existing “FOREIGN->RUB”
//     * rate stored in the DB.
//     */
//    private Mono<PaymentInvoiceReadDto> processUpdate(PaymentInvoice existingAllocation, PaymentInvoiceUpdateDto updateDto) {
//        return Mono.just(paymentInvoiceMapper.toCreateUpdateDto(updateDto, existingAllocation))
//                .flatMap(updatedDto -> enrichDtoForUpdate(updatedDto, existingAllocation))
//                .flatMap(enrichedDto -> validateAllocation(enrichedDto, existingAllocation))
//                .map(validatedDto -> paymentInvoiceMapper.update(existingAllocation, validatedDto))
//                .flatMap(paymentInvoiceRepository::save)
//                .map(paymentInvoiceMapper::toDto);
//    }
//
//    /*
//     *  Creation-specific enrich: fetch the Payment & Invoice, fetch the exchange rate from DB,
//     *  invert if needed (RUB->FOREIGN), and calculate convertedAmount.
//     */
//    private Mono<PaymentInvoiceCreateUpdateDto> enrichDtoForCreation(PaymentInvoiceCreateUpdateDto dto) {
//        // 1) Fetch Payment & Invoice
//        // 2) Call getExchangeRate(...) => returns always “FOREIGN->RUB”
//        // 3) Possibly invert for the math if it is RUB->FOREIGN
//        return fetchPaymentAndInvoice(dto)
//                .flatMap(tuple -> {
//                    PaymentReadDto paymentDto = tuple.getT1();
//                    InvoiceReadDto invoiceDto = tuple.getT2();
//
//                    validatePaymentAndInvoiceBelongToTheSameClient(paymentDto, invoiceDto);
//
//                    Integer currencyFromId = paymentDto.currencyId();
//                    Integer currencyToId = invoiceDto.currencyId();
//                    LocalDate paymentDate = paymentDto.paymentDate();
//
//                    return getExchangeRate(currencyFromId, currencyToId, paymentDate)
//                            .map(storedRate -> {
//                                boolean fromIsRub = currencyFromId.equals(RUB_CURRENCY_ID);
//                                boolean toIsRub   = currencyToId.equals(RUB_CURRENCY_ID);
//
//                                // If we do have RUB->FOREIGN, invert for the math
//                                BigDecimal effectiveRate = (fromIsRub && !toIsRub)
//                                        ? BigDecimal.ONE.divide(storedRate, 4, RoundingMode.HALF_UP)
//                                        : storedRate;
//
//                                BigDecimal convertedAmount = dto.allocatedAmount()
//                                        .multiply(effectiveRate)
//                                        .setScale(2, RoundingMode.HALF_UP);
//
//                                validateConvertedAmount(convertedAmount);
//
//                                // We store the DB's "FOREIGN->RUB" in exchangeRate
//                                return dto.toBuilder()
//                                        .currencyFromId(currencyFromId)
//                                        .currencyToId(currencyToId)
//                                        .exchangeRate(storedRate)
//                                        .convertedAmount(convertedAmount)
//                                        .build();
//                            });
//                });
//    }
//
//    /*
//     * Update-specific enrich: re-use the existingAllocation.getExchangeRate(),
//     * invert if needed, then multiply by the new allocatedAmount. We do not re-fetch
//     * from the DB or call getExchangeRate(...).
//     */
//    private Mono<PaymentInvoiceCreateUpdateDto> enrichDtoForUpdate(
//            PaymentInvoiceCreateUpdateDto dto,
//            PaymentInvoice existingAllocation
//    ) {
//        // Payment/Invoice might still be needed for certain validations, but not for exchangeRate
//        return fetchPaymentAndInvoice(dto)
//                .flatMap(tuple -> {
//                    // We'll re-use currencyFrom/To from the existing allocation
//                    Integer currencyFromId = existingAllocation.getCurrencyFromId();
//                    Integer currencyToId = existingAllocation.getCurrencyToId();
//
//                    BigDecimal storedRate = existingAllocation.getExchangeRate(); // e.g. “FOREIGN->RUB”
//
//                    boolean fromIsRub = currencyFromId.equals(RUB_CURRENCY_ID);
//                    boolean toIsRub   = currencyToId.equals(RUB_CURRENCY_ID);
//
//                    // If we do have RUB->FOREIGN, invert for the math
//                    BigDecimal effectiveRate = (fromIsRub && !toIsRub)
//                            ? BigDecimal.ONE.divide(storedRate, 4, RoundingMode.HALF_UP)
//                            : storedRate;
//
//                    BigDecimal convertedAmount = dto.allocatedAmount()
//                            .multiply(effectiveRate)
//                            .setScale(2, RoundingMode.HALF_UP);
//
//                    validateConvertedAmount(convertedAmount);
//
//                    return Mono.just(
//                            dto.toBuilder()
//                                    .currencyFromId(currencyFromId)
//                                    .currencyToId(currencyToId)
//                                    .exchangeRate(storedRate)       // keep storing the original
//                                    .convertedAmount(convertedAmount)
//                                    .build()
//                    );
//                });
//    }
//
//    /*
//     * ---------
//     * VALIDATION
//     * ---------
//     */
//
//    private Mono<PaymentInvoiceCreateUpdateDto> validateAllocation(
//            PaymentInvoiceCreateUpdateDto dto,
//            PaymentInvoice existingAllocation
//    ) {
//        // For final validations, we still need to know Payment & Invoice data
//        return fetchPaymentAndInvoice(dto)
//                .flatMap(tuple -> {
//                    PaymentReadDto paymentDto = tuple.getT1();
//                    InvoiceReadDto invoiceDto = tuple.getT2();
//
//                    // Adjust Payment/Invoice amounts if there's an existing allocation
//                    BigDecimal adjustedPaymentUnallocated = adjustPaymentUnallocated(paymentDto, existingAllocation);
//                    BigDecimal adjustedInvoiceOutstanding = adjustInvoiceOutstanding(invoiceDto, existingAllocation);
//
//                    return performValidations(dto, adjustedPaymentUnallocated, adjustedInvoiceOutstanding)
//                            .thenReturn(dto);
//                });
//    }
//
//    private BigDecimal adjustPaymentUnallocated(PaymentReadDto paymentDto, PaymentInvoice existingAllocation) {
//        BigDecimal paymentUnallocated = paymentDto.unallocatedAmount();
//        if (existingAllocation != null) {
//            paymentUnallocated = paymentUnallocated.add(existingAllocation.getAllocatedAmount());
//        }
//        return paymentUnallocated;
//    }
//
//    private BigDecimal adjustInvoiceOutstanding(InvoiceReadDto invoiceDto, PaymentInvoice existingAllocation) {
//        BigDecimal invoiceOutstandingBalance = invoiceDto.outstandingBalance();
//        if (existingAllocation != null) {
//            invoiceOutstandingBalance = invoiceOutstandingBalance.add(existingAllocation.getConvertedAmount());
//        }
//        return invoiceOutstandingBalance;
//    }
//
//    private Mono<Void> performValidations(
//            PaymentInvoiceCreateUpdateDto dto,
//            BigDecimal paymentUnallocated,
//            BigDecimal invoiceOutstandingBalance
//    ) {
//        return Mono.when(
//                validateAllocatedAmount(dto.allocatedAmount(), paymentUnallocated),
//                validateConvertedAmount(dto.convertedAmount(), invoiceOutstandingBalance),
//                validateCurrencySpecificAmounts(dto)
//        );
//    }
//
//    private void validatePaymentAndInvoiceBelongToTheSameClient(PaymentReadDto paymentDto, InvoiceReadDto invoiceDto) {
//        if (paymentDto.partnerId().compareTo(invoiceDto.partnerId()) != 0) {
//            throw new ValidationException("error.paymentInvoice.paymentAndInvoiceClientMismatch");
//        }
//    }
//
//    private Mono<Void> validateAllocatedAmount(BigDecimal allocatedAmount, BigDecimal paymentUnallocated) {
//        if (allocatedAmount.compareTo(paymentUnallocated) > 0) {
//            return Mono.error(new ValidationException(
//                    "error.paymentInvoice.allocatedAmountExceedsUnallocated"
//            ));
//        }
//        return Mono.empty();
//    }
//
//    private Mono<Void> validateConvertedAmount(BigDecimal convertedAmount, BigDecimal invoiceOutstandingBalance) {
//        if (convertedAmount.compareTo(invoiceOutstandingBalance) > 0) {
//            return Mono.error(new ValidationException(
//                    "error.paymentInvoice.convertedAmountExceedsOutstandingBalance"
//            ));
//        }
//        return Mono.empty();
//    }
//
//    private Mono<PaymentInvoiceCreateUpdateDto> validateCurrencySpecificAmounts(PaymentInvoiceCreateUpdateDto dto) {
//        if (dto.currencyFromId().equals(dto.currencyToId())) {
//            // Same currency => exchangeRate must be 1, allocated = converted
//            if (dto.exchangeRate().compareTo(BigDecimal.ONE) != 0) {
//                return Mono.error(new ValidationException("error.paymentInvoice.exchangeRateShouldBeOne"));
//            }
//            if (dto.allocatedAmount().compareTo(dto.convertedAmount()) != 0) {
//                return Mono.error(new ValidationException("error.paymentInvoice.amountsMustBeEqualForSameCurrency"));
//            }
//        } else {
//            // Different currency => exchange rate must be > 0
//            if (dto.exchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
//                return Mono.error(new ValidationException("error.paymentInvoice.validExchangeRateRequired"));
//            }
//        }
//        return Mono.just(dto);
//    }
//
//    private void validateConvertedAmount(BigDecimal convertedAmount) {
//        // Minimum allocated: must be >= 0.01, for example
//        if (convertedAmount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
//            throw new ValidationException("validation.paymentAllocation.convertedAmount.min");
//        }
//    }
//
//    /*
//     * ---------
//     * RATE LOGIC
//     * ---------
//     */
//
//    /**
//     * On creation, we fetch from DB. The return value is always a “FOREIGN->RUB” rate,
//     * so if we have RUB->FOREIGN, we invert for the math, but store the original in the DB field.
//     */
//    private Mono<BigDecimal> getExchangeRate(Integer currencyFromId, Integer currencyToId, LocalDate paymentDate) {
//        if (currencyFromId.equals(currencyToId)) {
//            return Mono.just(BigDecimal.ONE);
//        }
//
//        boolean fromIsRub = currencyFromId.equals(RUB_CURRENCY_ID);
//        boolean toIsRub   = currencyToId.equals(RUB_CURRENCY_ID);
//
//        if (fromIsRub && !toIsRub) {
//            // RUB->FOREIGN => we want the FOREIGN->RUB rate from DB
//            return fetchForeignToRubRate(currencyToId, paymentDate);
//        } else if (!fromIsRub && toIsRub) {
//            // FOREIGN->RUB => just fetch from DB
//            return fetchForeignToRubRate(currencyFromId, paymentDate);
//        } else {
//            // FOREIGN->FOREIGN => (USD->RUB) / (EUR->RUB) => USD->EUR
//            return Mono.zip(
//                    fetchForeignToRubRate(currencyFromId, paymentDate),
//                    fetchForeignToRubRate(currencyToId, paymentDate)
//            ).map(tuple -> {
//                BigDecimal fromRub = tuple.getT1();  // e.g. USD->RUB
//                BigDecimal toRub   = tuple.getT2();  // e.g. EUR->RUB
//                return fromRub.divide(toRub, 4, RoundingMode.HALF_UP);
//            });
//        }
//    }
//
//    private Mono<BigDecimal> fetchForeignToRubRate(Integer foreignCurrencyId, LocalDate paymentDate) {
//        return exchangeRateService.getForeignToRubExchangeRate(foreignCurrencyId, paymentDate)
//                .switchIfEmpty(
//                        exchangeRateService.fetchAndSaveRates(paymentDate)
//                                .then(exchangeRateService.getForeignToRubExchangeRate(foreignCurrencyId, paymentDate))
//                                .switchIfEmpty(Mono.error(new ExchangeRateRetrievalFailedException(
//                                        "error.exchangeRate.retrievalFailed",
//                                        foreignCurrencyId, paymentDate
//                                )))
//                );
//    }
//
//    /*
//     * ---------------
//     * HELPER METHODS
//     * ---------------
//     */
//
//    private Mono<Tuple2<PaymentReadDto, InvoiceReadDto>> fetchPaymentAndInvoice(PaymentInvoiceCreateUpdateDto dto) {
//        Mono<PaymentReadDto> paymentMono = paymentService.findById(dto.paymentId());
//        Mono<InvoiceReadDto> invoiceMono = invoiceService.findById(dto.invoiceId());
//        return Mono.zip(paymentMono, invoiceMono);
//    }
//}
