package ru.utlc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.constants.CacheNames;
import ru.utlc.dto.paymentstatus.PaymentStatusCreateUpdateDto;
import ru.utlc.dto.paymentstatus.PaymentStatusReadDto;
import ru.utlc.exception.PaymentStatusNotFoundException;
import ru.utlc.mapper.PaymentStatusMapper;
import ru.utlc.repository.PaymentStatusRepository;

import java.util.Objects;

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
public class PaymentStatusService {

    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentStatusMapper paymentStatusMapper;
    private final CacheManager cacheManager;
    private final ReactiveTransactionManager transactionManager;

    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

    @Cacheable(value = CacheNames.PAYMENT_STATUSES, key = "'all'")
    public Flux<PaymentStatusReadDto> findAll() {
        return paymentStatusRepository.findAll()
                .map(paymentStatusMapper::toDto)
                .doOnNext(dto -> Objects.requireNonNull(cacheManager.getCache(CacheNames.PAYMENT_STATUSES))
                        .put(dto.id(), dto));
    }

    @Cacheable(value = CacheNames.PAYMENT_STATUSES, key = "#p0")
    public Mono<PaymentStatusReadDto> findById(Integer id) {
        return paymentStatusRepository.findById(id)
                .switchIfEmpty(Mono.error(new PaymentStatusNotFoundException(id)))
                .map(paymentStatusMapper::toDto);
    }

    @CacheEvict(value = CacheNames.PAYMENT_STATUSES, key = "'all'")
    @CachePut(value = CacheNames.PAYMENT_STATUSES, key = "#result.id")
    public Mono<PaymentStatusReadDto> create(PaymentStatusCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(paymentStatusMapper::toEntity)
                .flatMap(paymentStatusRepository::save)
                .map(paymentStatusMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    @CacheEvict(value = CacheNames.PAYMENT_STATUSES, key = "'all'")
    @CachePut(value = CacheNames.PAYMENT_STATUSES, key = "#result.id")
    public Mono<PaymentStatusReadDto> update(Integer id, PaymentStatusCreateUpdateDto dto) {
        return paymentStatusRepository.findById(id)
                .switchIfEmpty(Mono.error(new PaymentStatusNotFoundException(id)))
                .flatMap(existingEntity -> {
                    paymentStatusMapper.update(existingEntity, dto);
                    return paymentStatusRepository.save(existingEntity);
                })
                .map(paymentStatusMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    @CacheEvict(value = CacheNames.PAYMENT_STATUSES, allEntries = true)
    public Mono<Boolean> delete(Integer id) {
        return paymentStatusRepository.findById(id)
                .flatMap(existingEntity -> paymentStatusRepository.delete(existingEntity)
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .as(transactionalOperator()::transactional);
    }
}
