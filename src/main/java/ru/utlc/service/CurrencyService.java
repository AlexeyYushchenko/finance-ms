package ru.utlc.service;

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
import ru.utlc.constants.CacheNames;
import ru.utlc.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.dto.currency.CurrencyReadDto;
import ru.utlc.exception.CurrencyNotFoundException;
import ru.utlc.mapper.CurrencyMapper;
import ru.utlc.model.Currency;
import ru.utlc.repository.CurrencyRepository;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = CacheNames.CURRENCIES, key = "'all'")
    public Flux<CurrencyReadDto> findAll() {
        return currencyRepository.findAll()
                .map(currencyMapper::toDto)
                .doOnNext(dto -> Objects.requireNonNull(cacheManager.getCache(CacheNames.CURRENCIES)).put(dto.id(), dto));
    }

    @Cacheable(value = CacheNames.CURRENCIES, key = "#p0")
    public Mono<CurrencyReadDto> findById(Integer id) {
        return currencyRepository.findById(id)
                .switchIfEmpty(Mono.error(new CurrencyNotFoundException(id)))
                .map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CacheNames.CURRENCIES, key = "'all'")
    @CachePut(value = CacheNames.CURRENCIES, key = "#result.id")
    public Mono<CurrencyReadDto> create(CurrencyCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(currencyMapper::toEntity)
                .flatMap(currencyRepository::save)
                .map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CacheNames.CURRENCIES, key = "'all'")
    @CachePut(value = CacheNames.CURRENCIES, key = "#result.id")
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
    @CacheEvict(value = CacheNames.CURRENCIES, allEntries = true)
    public Mono<Boolean> delete(Integer id) {
        return currencyRepository.findById(id)
                .flatMap(currency -> currencyRepository.delete(currency).thenReturn(true))
                .defaultIfEmpty(false);
    }

    public Flux<Currency> getEnabledCurrencies() {
        return currencyRepository.findByEnabledTrue();
    }
}
