package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.exception.AllocationNotFoundException;
import ru.utlc.financialmanagementservice.exception.InvoiceNotFoundException;
import ru.utlc.financialmanagementservice.exception.InvoiceUpdateException;
import ru.utlc.financialmanagementservice.mapper.InvoiceMapper;
import ru.utlc.financialmanagementservice.model.Invoice;
import ru.utlc.financialmanagementservice.model.PaymentInvoice;
import ru.utlc.financialmanagementservice.repository.InvoiceRepository;
import ru.utlc.financialmanagementservice.repository.PaymentInvoiceRepository;

import java.math.BigDecimal;
import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.INVOICES;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationService allocationService;
    private final PaymentInvoiceRepository paymentInvoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final CacheManager cacheManager;
    private final ClientBalanceService clientBalanceService;
    private final ReactiveTransactionManager transactionManager;

    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

//    @Cacheable(value = INVOICES, key = "'all'")
public Flux<InvoiceReadDto> findAll() {
    return invoiceRepository.findAll()
            .flatMap(this::populateAllocationFields)
            .map(invoiceMapper::toDto)
            .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(INVOICES)).put(entity.id(), entity));
}

//    @Cacheable(value = INVOICES, key = "#p0")
public Mono<InvoiceReadDto> findById(Long id) {
    return invoiceRepository.findById(id)
            .switchIfEmpty(Mono.error(new InvoiceNotFoundException("error.invoice.notFound", id)))
            .flatMap(this::populateAllocationFields)
            .map(invoiceMapper::toDto);
}

//    @CacheEvict(value = INVOICES, key = "'all'")
//    @CachePut(value = INVOICES, key = "#result.id")
    public Mono<InvoiceReadDto> create(InvoiceCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(invoiceMapper::toEntity)
                .flatMap(invoiceRepository::save)
                .flatMap(clientBalanceService::adjustBalance)
                .flatMap(this::populateAllocationFields)
                .map(invoiceMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    public Mono<InvoiceReadDto> update(Long id, InvoiceCreateUpdateDto dto) {
        return invoiceRepository.findById(id)
                .flatMap(existingInvoice -> verifyNoAllocations(existingInvoice)
                        .then(clientBalanceService.negateExistingInvoice(existingInvoice))
                        .flatMap(invoice -> updateAndSaveInvoice(invoice, dto))
                        .flatMap(clientBalanceService::updateBalanceForNewInvoice)
                )
                .flatMap(this::populateAllocationFields)
                .map(invoiceMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    private Mono<Void> verifyNoAllocations(Invoice existingInvoice) {
        return allocationService.hasAllocationsForInvoice(existingInvoice.getId())
                .flatMap(hasAllocations -> {
                    if (Boolean.TRUE.equals(hasAllocations)) {
                        return Mono.error(new InvoiceUpdateException("error.invoice.update"));
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public Mono<Invoice> updateAndSaveInvoice(Invoice existingInvoice, InvoiceCreateUpdateDto dto) {
        invoiceMapper.update(existingInvoice, dto);
        return invoiceRepository.save(existingInvoice);
    }

//    @CacheEvict(value = INVOICES, allEntries = true)
    public Mono<Boolean> delete(Long id) {
        return invoiceRepository.findById(id)
                .flatMap(invoice -> invoiceRepository.delete(invoice)
                        .then(clientBalanceService.adjustBalanceForInvoiceDeletion(invoice))
                        .thenReturn(true)
                )
                .defaultIfEmpty(false)
                .as(transactionalOperator()::transactional);
    }

    private Mono<Invoice> populateAllocationFields(Invoice invoice) {
        return paymentInvoiceRepository.findAllByInvoiceId(invoice.getId())
                .map(PaymentInvoice::getConvertedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .defaultIfEmpty(BigDecimal.ZERO)
                .map(totalAllocated -> {
                    invoice.setAmountPaid(totalAllocated);
                    invoice.setOutstandingBalance(invoice.getTotalAmount().subtract(totalAllocated));
                    return invoice;
                });
    }
}