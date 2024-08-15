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
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.mapper.PaymentMapper;
import ru.utlc.financialmanagementservice.repository.PaymentRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.PAYMENTS;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = PAYMENTS, key = "'all'")
    public Flux<PaymentReadDto> findAll() {
        return paymentRepository.findAll()
                .map(paymentMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(PAYMENTS)).put(entity.id(), entity));
    }

    @Cacheable(value = PAYMENTS, key = "#p0")
    public Mono<PaymentReadDto> findById(Long id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENTS, key = "'all'")
    @CachePut(value = PAYMENTS, key = "#result.id")
    public Mono<PaymentReadDto> create(PaymentCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(paymentMapper::toEntity)
                .flatMap(paymentRepository::save)
                .map(paymentMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENTS, key = "'all'")
    @CachePut(value = PAYMENTS, key = "#result.id")
    public Mono<PaymentReadDto> update(Long id, PaymentCreateUpdateDto dto) {
        return paymentRepository.findById(id)
                .flatMap(entity -> Mono.just(paymentMapper.update(entity, dto)))
                .flatMap(paymentRepository::save)
                .map(paymentMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENTS, allEntries = true)
    public Mono<Boolean> delete(Long id) {
        return paymentRepository.findById(id)
                .flatMap(payment -> paymentRepository.delete(payment)
                        .thenReturn(true))
                .defaultIfEmpty(false);
    }
}