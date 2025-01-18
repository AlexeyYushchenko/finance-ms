package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceNotFoundException;
import ru.utlc.financialmanagementservice.exception.PaymentTypeNotFoundException;
import ru.utlc.financialmanagementservice.mapper.PaymentTypeMapper;
import ru.utlc.financialmanagementservice.repository.PaymentTypeRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.PAYMENT_TYPES;

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
@Transactional(readOnly = true)
public class PaymentTypeService {
    private final PaymentTypeRepository paymentTypeRepository;
    private final PaymentTypeMapper paymentTypeMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = PAYMENT_TYPES, key = "'all'")
    public Flux<PaymentTypeReadDto> findAll() {
        return paymentTypeRepository.findAll()
                .map(paymentTypeMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(PAYMENT_TYPES)).put(entity.id(), entity));
    }

    @Cacheable(value = PAYMENT_TYPES, key = "#p0")
    public Mono<PaymentTypeReadDto> findById(Integer id) {
        return paymentTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new PaymentTypeNotFoundException("error.paymentType.notFound", id)))
                .map(paymentTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENT_TYPES, key = "'all'")
    @CachePut(value = PAYMENT_TYPES, key = "#result.id")
    public Mono<PaymentTypeReadDto> create(PaymentTypeCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(paymentTypeMapper::toEntity)
                .flatMap(paymentTypeRepository::save)
                .map(paymentTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENT_TYPES, key = "'all'")
    @CachePut(value = PAYMENT_TYPES, key = "#result.id")
    public Mono<PaymentTypeReadDto> update(Integer id, PaymentTypeCreateUpdateDto dto) {
        return paymentTypeRepository.findById(id)
                .flatMap(entity -> Mono.just(paymentTypeMapper.update(entity, dto)))
                .flatMap(paymentTypeRepository::save)
                .map(paymentTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENT_TYPES, allEntries = true)
    //todo improve by selectively deleting only the cached entity while updating 'all'.
    public Mono<Boolean> delete(Integer id) {
        return paymentTypeRepository.findById(id)
                .flatMap(paymentType -> paymentTypeRepository.delete(paymentType)
                        .thenReturn(true))
                .defaultIfEmpty(false);
    }
}
