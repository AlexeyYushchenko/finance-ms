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
import ru.utlc.financialmanagementservice.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.exception.CurrencyNotFoundException;
import ru.utlc.financialmanagementservice.mapper.CurrencyMapper;
import ru.utlc.financialmanagementservice.model.Currency;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.CURRENCIES;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = CURRENCIES, key = "'all'")
    public Flux<CurrencyReadDto> findAll() {
        return currencyRepository.findAll()
                .map(currencyMapper::toDto)
                .doOnNext(dto -> Objects.requireNonNull(cacheManager.getCache(CURRENCIES)).put(dto.id(), dto));
    }

    @Cacheable(value = CURRENCIES, key = "#p0")
    public Mono<CurrencyReadDto> findById(Integer id) {
        return currencyRepository.findById(id)
                .switchIfEmpty(Mono.error(new CurrencyNotFoundException(id)))
                .map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CURRENCIES, key = "'all'")
    @CachePut(value = CURRENCIES, key = "#result.id")
    public Mono<CurrencyReadDto> create(CurrencyCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(currencyMapper::toEntity)
                .flatMap(currencyRepository::save)
                .map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CURRENCIES, key = "'all'")
    @CachePut(value = CURRENCIES, key = "#result.id")
    public Mono<CurrencyReadDto> update(Integer id, CurrencyCreateUpdateDto dto) {
        return currencyRepository.findById(id)
                .switchIfEmpty(Mono.error(new CurrencyNotFoundException(id)))
                .flatMap(existing -> {
                    currencyMapper.update(existing, dto);
                    return currencyRepository.save(existing);
                })
                .map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CURRENCIES, allEntries = true)
    public Mono<Boolean> delete(Integer id) {
        return currencyRepository.findById(id)
                .flatMap(currency -> currencyRepository.delete(currency).thenReturn(true))
                .defaultIfEmpty(false);
    }

    public Flux<Currency> getEnabledCurrencies() {
        return currencyRepository.findByEnabledTrue();
    }
}
