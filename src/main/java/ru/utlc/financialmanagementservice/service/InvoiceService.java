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
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.integration.ClientService;
import ru.utlc.financialmanagementservice.mapper.CurrencyMapper;
import ru.utlc.financialmanagementservice.mapper.InvoiceMapper;
import ru.utlc.financialmanagementservice.mapper.InvoiceStatusMapper;
import ru.utlc.financialmanagementservice.mapper.ServiceTypeMapper;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;
import ru.utlc.financialmanagementservice.repository.InvoiceRepository;
import ru.utlc.financialmanagementservice.repository.InvoiceStatusRepository;
import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;

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
@Transactional(readOnly = true)
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceStatusRepository invoiceStatusRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final CacheManager cacheManager;
    private final ClientService clientService;
    private final ServiceTypeMapper serviceTypeMapper;
    private final CurrencyMapper currencyMapper;
    private final InvoiceStatusMapper invoiceStatusMapper;

    @Cacheable(value = INVOICES, key = "'all'")
    public Flux<InvoiceReadDto> findAll() {
        return invoiceRepository.findAll()
                .map(invoiceMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(INVOICES)).put(entity.id(), entity));
    }

    @Cacheable(value = INVOICES, key = "#p0")
    public Mono<InvoiceReadDto> findById(Long id) {
        return invoiceRepository.findById(id)
                .map(invoiceMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICES, key = "'all'")
    @CachePut(value = INVOICES, key = "#result.id")
    public Mono<InvoiceReadDto> create(InvoiceCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(invoiceMapper::toEntity)
//                .flatMap(entity -> setUpOtherEntitiesToMainEntity(entity, dto)) //смотри комментарий в todo ниже
                .flatMap(invoiceRepository::save)
                .map(invoiceMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICES, key = "'all'")
    @CachePut(value = INVOICES, key = "#result.id")
    public Mono<InvoiceReadDto> update(Long id, InvoiceCreateUpdateDto dto) {
        return invoiceRepository.findById(id)
                .flatMap(entity -> Mono.just(invoiceMapper.update(entity, dto)))
//                .flatMap(entity -> setUpOtherEntitiesToMainEntity(entity, dto)) //смотри комментарий в todo ниже
                .flatMap(invoiceRepository::save)
                .map(invoiceMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICES, allEntries = true)
    public Mono<Boolean> delete(Long id) {
        return invoiceRepository.findById(id)
                .flatMap(invoice -> invoiceRepository.delete(invoice)
                        .thenReturn(true))
                .defaultIfEmpty(false);
    }

//    todo ИМХО: Если мы выдаем простой DTO с ID вместо целых сущностей, то и эта часть не нужна.
//    private Mono<Invoice> setUpOtherEntitiesToMainEntity(Invoice entity, InvoiceCreateUpdateDto dto) {
//        Mono<InvoiceStatus> invoiceStatusMono = dto.statusId() != null
//                ? invoiceStatusRepository.findById(dto.statusId())
//                : Mono.empty();
//
//        Mono<Currency> currencyMono = dto.currencyId() != null
//                ? currencyRepository.findById(dto.currencyId())  // Corrected to fetch currency by currencyId
//                : Mono.empty();
//
//        Mono<ServiceType> serviceTypeMono = dto.serviceTypeId() != null
//                ? serviceTypeRepository.findById(dto.serviceTypeId())
//                : Mono.empty();
//
//        return Mono.zip(invoiceStatusMono, currencyMono, serviceTypeMono)
//                .map(tuple -> {
//                    InvoiceStatus invoiceStatus = tuple.getT1();
//                    Currency currency = tuple.getT2();
//                    ServiceType serviceType = tuple.getT3();
//
//                    entity.setStatusId(invoiceStatus.getId());
//                    entity.setCurrencyId(currency.getId());
//                    entity.setServiceTypeId(serviceType.getId());
//                    return entity;
//                });
//    }

}