//package ru.utlc.financialmanagementservice.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.CachePut;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.ReactiveTransactionManager;
//import org.springframework.transaction.reactive.TransactionalOperator;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import ru.utlc.financialmanagementservice.dto.partnertype.PartnerTypeCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.partnertype.PartnerTypeReadDto;
//import ru.utlc.financialmanagementservice.exception.PartnerTypeNotFoundException;
//import ru.utlc.financialmanagementservice.mapper.PartnerTypeMapper;
//import ru.utlc.financialmanagementservice.repository.PartnerTypeRepository;
//
//import java.util.Objects;
//
//import static ru.utlc.financialmanagementservice.constants.CacheNames.PARTNER_TYPES;
//
///*
// * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
// * Licensed under Proprietary License.
// *
// * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
// * Date: 2024-08-19
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class PartnerTypeService {
//    private final PartnerTypeRepository partnerTypeRepository;
//    private final PartnerTypeMapper partnerTypeMapper;
//    private final CacheManager cacheManager;
//    private final ReactiveTransactionManager transactionManager;
//
//    private TransactionalOperator transactionalOperator() {
//        return TransactionalOperator.create(transactionManager);
//    }
//
//    @Cacheable(value = PARTNER_TYPES, key = "'all'")
//    public Flux<PartnerTypeReadDto> findAll() {
//        return partnerTypeRepository.findAll()
//                .map(partnerTypeMapper::toDto)
//                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(PARTNER_TYPES)).put(entity.id(), entity));
//    }
//
//    @Cacheable(value = PARTNER_TYPES, key = "#p0")
//    public Mono<PartnerTypeReadDto> findById(Integer id) {
//        return partnerTypeRepository.findById(id)
//                .switchIfEmpty(Mono.error(new PartnerTypeNotFoundException(id)))
//                .map(partnerTypeMapper::toDto);
//    }
//
//    @CacheEvict(value = PARTNER_TYPES, key = "'all'")
//    @CachePut(value = PARTNER_TYPES, key = "#result.id")
//    public Mono<PartnerTypeReadDto> create(PartnerTypeCreateUpdateDto dto) {
//        return Mono.just(dto)
//                .map(partnerTypeMapper::toEntity)
//                .flatMap(partnerTypeRepository::save)
//                .map(partnerTypeMapper::toDto)
//                .as(transactionalOperator()::transactional);
//    }
//
//    @CacheEvict(value = PARTNER_TYPES, key = "'all'")
//    @CachePut(value = PARTNER_TYPES, key = "#result.id")
//    public Mono<PartnerTypeReadDto> update(Integer id, PartnerTypeCreateUpdateDto dto) {
//        return partnerTypeRepository.findById(id)
//                .switchIfEmpty(Mono.error(new PartnerTypeNotFoundException(id)))
//                .flatMap(existingEntity -> {
//                    partnerTypeMapper.update(existingEntity, dto);
//                    return partnerTypeRepository.save(existingEntity);
//                })
//                .map(partnerTypeMapper::toDto)
//                .as(transactionalOperator()::transactional);
//    }
//
//    @CacheEvict(value = PARTNER_TYPES, allEntries = true)
//    public Mono<Boolean> delete(Integer id) {
//        return partnerTypeRepository.findById(id)
//                .flatMap(existingEntity -> partnerTypeRepository.delete(existingEntity)
//                        .thenReturn(true))
//                .defaultIfEmpty(false)
//                .as(transactionalOperator()::transactional);
//    }
//}