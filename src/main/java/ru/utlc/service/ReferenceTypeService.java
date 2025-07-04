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
import ru.utlc.dto.referencetype.ReferenceTypeCreateUpdateDto;
import ru.utlc.dto.referencetype.ReferenceTypeReadDto;
import ru.utlc.exception.ReferenceTypeNotFoundException;
import ru.utlc.mapper.ReferenceTypeMapper;
import ru.utlc.repository.ReferenceTypeRepository;

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
public class ReferenceTypeService {
    private final ReferenceTypeRepository referenceTypeRepository;
    private final ReferenceTypeMapper referenceTypeMapper;
    private final CacheManager cacheManager;
    private final ReactiveTransactionManager transactionManager;

    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

    @Cacheable(value = CacheNames.REFERENCE_TYPES, key = "'all'")
    public Flux<ReferenceTypeReadDto> findAll() {
        return referenceTypeRepository.findAll()
                .map(referenceTypeMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(CacheNames.REFERENCE_TYPES)).put(entity.id(), entity));
    }

    @Cacheable(value = CacheNames.REFERENCE_TYPES, key = "#p0")
    public Mono<ReferenceTypeReadDto> findById(Integer id) {
        return referenceTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ReferenceTypeNotFoundException(id)))
                .map(referenceTypeMapper::toDto);
    }

    @CacheEvict(value = CacheNames.REFERENCE_TYPES, key = "'all'")
    @CachePut(value = CacheNames.REFERENCE_TYPES, key = "#result.id")
    public Mono<ReferenceTypeReadDto> create(ReferenceTypeCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(referenceTypeMapper::toEntity)
                .flatMap(referenceTypeRepository::save)
                .map(referenceTypeMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    @CacheEvict(value = CacheNames.REFERENCE_TYPES, key = "'all'")
    @CachePut(value = CacheNames.REFERENCE_TYPES, key = "#result.id")
    public Mono<ReferenceTypeReadDto> update(Integer id, ReferenceTypeCreateUpdateDto dto) {
        return referenceTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ReferenceTypeNotFoundException(id)))
                .flatMap(existingEntity -> {
                    referenceTypeMapper.update(existingEntity, dto);
                    return referenceTypeRepository.save(existingEntity);
                })
                .map(referenceTypeMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    @CacheEvict(value = CacheNames.REFERENCE_TYPES, allEntries = true)
    public Mono<Boolean> delete(Integer id) {
        return referenceTypeRepository.findById(id)
                .flatMap(existingEntity -> referenceTypeRepository.delete(existingEntity)
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .as(transactionalOperator()::transactional);
    }
}
