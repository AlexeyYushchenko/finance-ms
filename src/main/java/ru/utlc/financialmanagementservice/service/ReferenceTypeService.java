package ru.utlc.financialmanagementservice.service;

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
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.financialmanagementservice.exception.ServiceTypeNotFoundException;
import ru.utlc.financialmanagementservice.mapper.ServiceTypeMapper;
import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.SERVICE_TYPES;

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
public class ServiceTypeService {
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeMapper serviceTypeMapper;
    private final CacheManager cacheManager;
    private final ReactiveTransactionManager transactionManager;

    private TransactionalOperator transactionalOperator() {
        return TransactionalOperator.create(transactionManager);
    }

    @Cacheable(value = SERVICE_TYPES, key = "'all'")
    public Flux<ServiceTypeReadDto> findAll() {
        return serviceTypeRepository.findAll()
                .map(serviceTypeMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(SERVICE_TYPES)).put(entity.id(), entity));
    }

    @Cacheable(value = SERVICE_TYPES, key = "#p0")
    public Mono<ServiceTypeReadDto> findById(Integer id) {
        return serviceTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ServiceTypeNotFoundException("error.serviceType.notFound", id)))
                .map(serviceTypeMapper::toDto);
    }

    @CacheEvict(value = SERVICE_TYPES, key = "'all'")
    @CachePut(value = SERVICE_TYPES, key = "#result.id")
    public Mono<ServiceTypeReadDto> create(ServiceTypeCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(serviceTypeMapper::toEntity)
                .flatMap(serviceTypeRepository::save)
                .map(serviceTypeMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    @CacheEvict(value = SERVICE_TYPES, key = "'all'")
    @CachePut(value = SERVICE_TYPES, key = "#result.id")
    public Mono<ServiceTypeReadDto> update(Integer id, ServiceTypeCreateUpdateDto dto) {
        return serviceTypeRepository.findById(id)
                .switchIfEmpty(Mono.error(new ServiceTypeNotFoundException("error.serviceType.notFound", id)))
                .flatMap(existingEntity -> {
                    serviceTypeMapper.update(existingEntity, dto);
                    return serviceTypeRepository.save(existingEntity);
                })
                .map(serviceTypeMapper::toDto)
                .as(transactionalOperator()::transactional);
    }

    @CacheEvict(value = SERVICE_TYPES, allEntries = true)
    public Mono<Boolean> delete(Integer id) {
        return serviceTypeRepository.findById(id)
                .flatMap(existingEntity -> serviceTypeRepository.delete(existingEntity)
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .as(transactionalOperator()::transactional);
    }
}
