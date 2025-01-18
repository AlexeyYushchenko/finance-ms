package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceNotFoundException;
import ru.utlc.financialmanagementservice.exception.PaymentNotFoundException;
import ru.utlc.financialmanagementservice.exception.PaymentUpdateException;
import ru.utlc.financialmanagementservice.mapper.PaymentMapper;
import ru.utlc.financialmanagementservice.model.Payment;
import ru.utlc.financialmanagementservice.model.PaymentAllocationView;
import ru.utlc.financialmanagementservice.repository.PaymentAllocationViewRepository;
import ru.utlc.financialmanagementservice.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.PAYMENTS;

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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final CacheManager cacheManager;
    private final ReactiveTransactionManager transactionManager;
    private final ClientBalanceService clientBalanceService;
    private final PaymentAllocationViewRepository paymentAllocationViewRepository;
    private final PaymentAllocationService allocationService;

    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

    //    @Cacheable(value = PAYMENTS, key = "'all'")
    public Flux<PaymentReadDto> findAll() {
        return paymentRepository.findAll()
                .flatMap(payment -> getPaymentWithAllocations(payment.getId()))
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(PAYMENTS)).put(entity.id(), entity));
    }

    //    @Cacheable(value = PAYMENTS, key = "#p0")
    public Mono<PaymentReadDto> findById(Long id) {
        return getPaymentWithAllocations(id);
    }

    //    @CacheEvict(value = PAYMENTS, key = "'all'")
//    @CachePut(value = PAYMENTS, key = "#result.id")
    public Mono<PaymentReadDto> create(PaymentCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(paymentMapper::toEntity)
                .map(Payment::calculateTotalAmount)
                .flatMap(paymentRepository::save)
                .flatMap(clientBalanceService::adjustBalance)
                .flatMap(savedPayment -> getPaymentWithAllocations(savedPayment.getId()))
                .as(transactionalOperator()::transactional);
    }

    //    @CacheEvict(value = PAYMENTS, key = "'all'")
//    @CachePut(value = PAYMENTS, key = "#result.id", condition = "#result != null && #result.blockOptional().isPresent()")
    public Mono<PaymentReadDto> update(Long id, PaymentCreateUpdateDto dto) {
        return paymentRepository.findById(id)
                .flatMap(existingPayment -> verifyNoAllocations(existingPayment)
                        .then(updateAndSavePayment(existingPayment, dto))
                        .flatMap(clientBalanceService::updateBalanceForNewPayment)
                )
                .flatMap(updatedPayment -> getPaymentWithAllocations(updatedPayment.getId()))
                .as(transactionalOperator()::transactional);
    }

    private Mono<Void> verifyNoAllocations(Payment existingPayment) {
        return allocationService.hasAllocationsForPayment(existingPayment.getId())
                .flatMap(hasAllocations -> {
                    if (Boolean.TRUE.equals(hasAllocations)) {
                        return Mono.error(new PaymentUpdateException("error.payment.update"));
                    } else {
                        return Mono.empty();
                    }
                });
    }

    public Mono<Payment> updateAndSavePayment(Payment existingPayment, PaymentCreateUpdateDto dto) {
        paymentMapper.update(existingPayment, dto);
        existingPayment.calculateTotalAmount();
        return paymentRepository.save(existingPayment);
    }

    //    @CacheEvict(value = PAYMENTS, allEntries = true)
    public Mono<Boolean> delete(Long id) {
        return paymentRepository.findById(id)
                .flatMap(payment -> paymentRepository.delete(payment)
                        .then(clientBalanceService.adjustBalanceForPaymentDeletion(payment))
                        .thenReturn(true)
                )
                .defaultIfEmpty(false)
                .as(transactionalOperator()::transactional);
    }

    public Mono<PaymentReadDto> getPaymentWithAllocations(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("error.payment.notFound", paymentId)))
                .flatMap(payment -> paymentAllocationViewRepository.findById(paymentId)
                        .defaultIfEmpty(new PaymentAllocationView(paymentId, payment.getTotalAmount(), BigDecimal.ZERO, payment.getTotalAmount(), false))
                        .map(allocationView -> {
                            // Update payment fields with allocation data
                            payment.setAllocatedAmount(allocationView.getAllocatedAmount());
                            payment.setUnallocatedAmount(allocationView.getUnallocatedAmount());
                            payment.setIsFullyAllocated(allocationView.getIsFullyAllocated());
                            return payment;
                        })
                )
                .map(paymentMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(PAYMENTS)).put(entity.id(), entity));
    }
}