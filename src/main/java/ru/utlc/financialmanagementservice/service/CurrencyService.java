package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.exception.CurrencyCreationException;
import ru.utlc.financialmanagementservice.mapper.CurrencyMapper;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;

import java.util.List;
import java.util.Optional;

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
    public List<CurrencyReadDto> findAll() {
        List<CurrencyReadDto> list = currencyRepository.findAll().stream()
                .map(currencyMapper::toDto)
                .toList();

        list.forEach(entity -> cacheManager.getCache(CURRENCIES).put(entity.id(), entity));
        return list;
    }

    @Cacheable(value = CURRENCIES, key = "#p0")
    public Optional<CurrencyReadDto> findById(Integer id) {
        return currencyRepository.findById(id).map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CURRENCIES, allEntries = true)
    @CachePut(value = CURRENCIES, key = "#result.id")
    public CurrencyReadDto create(CurrencyCreateUpdateDto createUpdateDto) throws CurrencyCreationException {
        return Optional.of(createUpdateDto)
                .map(currencyMapper::toEntity)
                .map(currencyRepository::save)
                .map(currencyMapper::toDto)
                .orElseThrow(() -> new CurrencyCreationException("error.entity.currency.creation"));
    }

    @Transactional
    @CacheEvict(value = CURRENCIES, allEntries = true)
    @CachePut(value = CURRENCIES, key = "#result.id")
    public Optional<CurrencyReadDto> update(Integer id, CurrencyCreateUpdateDto dto) {
        return currencyRepository.findById(id)
                .map(entity -> currencyMapper.update(entity, dto))
                .map(currencyRepository::saveAndFlush)
                .map(currencyMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = CURRENCIES, allEntries = true)
    public boolean delete(Integer id) {
        return currencyRepository.findById(id)
                .map(currency -> {
                    currencyRepository.delete(currency);
                    currencyRepository.flush();
                    return true;
                })
                .orElse(false);
    }
}
