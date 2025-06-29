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
import ru.utlc.financialmanagementservice.dto.invoicestatus.InvoiceStatusCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoicestatus.InvoiceStatusReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceStatusNotFoundException;
import ru.utlc.financialmanagementservice.mapper.InvoiceStatusMapper;
import ru.utlc.financialmanagementservice.repository.InvoiceStatusRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.INVOICE_STATUSES;

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
public class InvoiceStatusService {
    private final InvoiceStatusRepository invoiceStatusRepository;
    private final InvoiceStatusMapper invoiceStatusMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = INVOICE_STATUSES, key = "'all'")
    public Flux<InvoiceStatusReadDto> findAll() {
        return invoiceStatusRepository.findAll()
                .map(invoiceStatusMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(INVOICE_STATUSES)).put(entity.id(), entity));
    }

    @Cacheable(value = INVOICE_STATUSES, key = "#p0")
    public Mono<InvoiceStatusReadDto> findById(Integer id) {
        return invoiceStatusRepository.findById(id)
                .switchIfEmpty(Mono.error(new InvoiceStatusNotFoundException(id)))
                .map(invoiceStatusMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICE_STATUSES, key = "'all'")
    @CachePut(value = INVOICE_STATUSES, key = "#result.id")
    public Mono<InvoiceStatusReadDto> create(InvoiceStatusCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(invoiceStatusMapper::toEntity)
                .flatMap(invoiceStatusRepository::save)
                .map(invoiceStatusMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICE_STATUSES, key = "'all'")
    @CachePut(value = INVOICE_STATUSES, key = "#result.id")
    public Mono<InvoiceStatusReadDto> update(Integer id, InvoiceStatusCreateUpdateDto dto) {
        return invoiceStatusRepository.findById(id)
                .switchIfEmpty(Mono.error(new InvoiceStatusNotFoundException(id)))
                .flatMap(entity -> Mono.just(invoiceStatusMapper.update(entity, dto)))
                .flatMap(invoiceStatusRepository::save)
                .map(invoiceStatusMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICE_STATUSES, allEntries = true)
    //todo improve by selectively deleting only the cached entity while updating 'all'.
    public Mono<Boolean> delete(Integer id) {
        return invoiceStatusRepository.findById(id)
                .flatMap(invoiceStatus -> invoiceStatusRepository.delete(invoiceStatus)
                        .thenReturn(true))
                .defaultIfEmpty(false);
    }
}
