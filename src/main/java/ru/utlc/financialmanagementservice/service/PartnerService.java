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
//import ru.utlc.financialmanagementservice.dto.partner.PartnerCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.partner.PartnerReadDto;
//import ru.utlc.financialmanagementservice.exception.PartnerNotFoundException;
//import ru.utlc.financialmanagementservice.mapper.PartnerMapper;
//import ru.utlc.financialmanagementservice.repository.PartnerRepository;
//
//import java.util.Objects;
//
//import static ru.utlc.financialmanagementservice.constants.CacheNames.PARTNERS;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class PartnerService {
//
//    private final PartnerRepository partnerRepository;
//    private final PartnerMapper partnerMapper;
//    private final CacheManager cacheManager;
//    private final ReactiveTransactionManager transactionManager;
//
//    private TransactionalOperator transactionalOperator() {
//        return TransactionalOperator.create(transactionManager);
//    }
//
//    @Cacheable(value = PARTNERS, key = "'all'")
//    public Flux<PartnerReadDto> findAll() {
//        return partnerRepository.findAll()
//                .map(partnerMapper::toDto)
//                // For each partner in the Flux, we manually put them into the cache by ID
//                .doOnNext(dto -> Objects.requireNonNull(cacheManager.getCache(PARTNERS))
//                        .put(dto.id(), dto));
//    }
//
//    @Cacheable(value = PARTNERS, key = "#p0")
//    public Mono<PartnerReadDto> findById(Long id) {
//        return partnerRepository.findById(id)
//                .switchIfEmpty(Mono.error(new PartnerNotFoundException(id)))
//                .map(partnerMapper::toDto);
//    }
//
//    @CacheEvict(value = PARTNERS, key = "'all'")
//    @CachePut(value = PARTNERS, key = "#result.id")
//    public Mono<PartnerReadDto> create(PartnerCreateUpdateDto dto) {
//        return Mono.just(dto)
//                .map(partnerMapper::toEntity)
//                .flatMap(partnerRepository::save)
//                .map(partnerMapper::toDto)
//                .as(transactionalOperator()::transactional);
//    }
//
//    @CacheEvict(value = PARTNERS, key = "'all'")
//    @CachePut(value = PARTNERS, key = "#result.id")
//    public Mono<PartnerReadDto> update(Long id, PartnerCreateUpdateDto dto) {
//        return partnerRepository.findById(id)
//                .switchIfEmpty(Mono.error(new PartnerNotFoundException(id)))
//                .flatMap(existing -> {
//                    partnerMapper.update(existing, dto);
//                    return partnerRepository.save(existing);
//                })
//                .map(partnerMapper::toDto)
//                .as(transactionalOperator()::transactional);
//    }
//
//    @CacheEvict(value = PARTNERS, allEntries = true)
//    public Mono<Boolean> delete(Long id) {
//        return partnerRepository.findById(id)
//                .flatMap(__ -> partnerRepository.deleteById(id).thenReturn(true))
//                .defaultIfEmpty(false)
//                .as(transactionalOperator()::transactional);
//    }
//}
