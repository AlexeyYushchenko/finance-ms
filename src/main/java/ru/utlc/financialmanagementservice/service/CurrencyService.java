package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.exception.CurrencyNotFoundException;
import ru.utlc.financialmanagementservice.mapper.CurrencyMapper;
import ru.utlc.financialmanagementservice.model.Currency;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.CURRENCIES;

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
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;
    private final ReactiveTransactionManager transactionManager;

    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

    public Flux<CurrencyReadDto> findAll() {
        return currencyRepository.findAll()
                .map(currencyMapper::toDto);
//                .doOnNext(dto ->
//                        Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).put(dto.id(), dto)
//                );
    }


    public Mono<CurrencyReadDto> findById(Integer id) {
        return currencyRepository.findById(id)
                .switchIfEmpty(Mono.error(new CurrencyNotFoundException("error.currency.notFound", id)))
                .map(currencyMapper::toDto);
//                .doOnNext(dto ->
//                        Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).put(dto.id(), dto)
//                );
    }

    public Mono<CurrencyReadDto> create(CurrencyCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(currencyMapper::toEntity)
                .flatMap(currencyRepository::save)
                .map(currencyMapper::toDto)
//                .doOnNext(savedDto -> {
//                    // Evict old 'all' results and put the new entry
//                    Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).evict("all");
//                    Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).put(savedDto.id(), savedDto);
//                })
                .as(transactionalOperator()::transactional); // Wrap in reactive transaction if needed
    }

    public Mono<CurrencyReadDto> update(Integer id, CurrencyCreateUpdateDto dto) {
        return currencyRepository.findById(id)
                .switchIfEmpty(Mono.error(new CurrencyNotFoundException("error.currency.notFound", id)))
                .flatMap(existing -> {
                    currencyMapper.update(existing, dto);
                    return currencyRepository.save(existing);
                })
                .map(currencyMapper::toDto)
//                .doOnNext(updatedDto -> {
//                    // Evict old 'all' results and cache the updated currency
//                    Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).evict("all");
//                    Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).put(updatedDto.id(), updatedDto);
//                })
                .as(transactionalOperator()::transactional);
    }

    public Mono<Boolean> delete(Integer id) {
        return currencyRepository.findById(id)
                .flatMap(currency -> currencyRepository.delete(currency).thenReturn(true))
                .defaultIfEmpty(false)
//                .doOnNext(deleted -> {
//                    if (deleted) {
//                        // If successfully deleted, clear all cache entries
//                        Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).clear();
//                    }})
                .as(transactionalOperator()::transactional);
    }

    public Flux<Currency> getEnabledCurrencies() {
        return currencyRepository.findByEnabledTrue();
    }

}
