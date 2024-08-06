package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.financialmanagementservice.exception.PaymentTypeCreationException;
import ru.utlc.financialmanagementservice.mapper.PaymentTypeMapper;
import ru.utlc.financialmanagementservice.repository.PaymentTypeRepository;

import java.util.List;
import java.util.Optional;

import static ru.utlc.financialmanagementservice.constants.CacheNames.PAYMENT_TYPES;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentTypeService {
    private final PaymentTypeRepository paymentTypeRepository;
    private final PaymentTypeMapper paymentTypeMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = PAYMENT_TYPES, key = "'all'")
    public List<PaymentTypeReadDto> findAll() {
        List<PaymentTypeReadDto> list = paymentTypeRepository.findAll().stream()
                .map(paymentTypeMapper::toDto)
                .toList();

        list.forEach(entity -> cacheManager.getCache(PAYMENT_TYPES).put(entity.id(), entity));
        return list;
    }

    @Cacheable(value = PAYMENT_TYPES, key = "#p0")
    public Optional<PaymentTypeReadDto> findById(Integer id) {
        return paymentTypeRepository.findById(id).map(paymentTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENT_TYPES, allEntries = true)
    @CachePut(value = PAYMENT_TYPES, key = "#result.id")
    public PaymentTypeReadDto create(PaymentTypeCreateUpdateDto createUpdateDto) throws PaymentTypeCreationException {
        return Optional.of(createUpdateDto)
                .map(paymentTypeMapper::toEntity)
                .map(paymentTypeRepository::save)
                .map(paymentTypeMapper::toDto)
                .orElseThrow(() -> new PaymentTypeCreationException("error.entity.paymentType.creation"));
    }

    @Transactional
    @CacheEvict(value = PAYMENT_TYPES, allEntries = true)
    @CachePut(value = PAYMENT_TYPES, key = "#result.id")
    public Optional<PaymentTypeReadDto> update(Integer id, PaymentTypeCreateUpdateDto dto) {
        return paymentTypeRepository.findById(id)
                .map(entity -> paymentTypeMapper.update(entity, dto))
                .map(paymentTypeRepository::saveAndFlush)
                .map(paymentTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = PAYMENT_TYPES, allEntries = true)
    public boolean delete(Integer id) {
        return paymentTypeRepository.findById(id)
                .map(paymentType -> {
                    paymentTypeRepository.delete(paymentType);
                    paymentTypeRepository.flush();
                    return true;
                })
                .orElse(false);
    }
}
