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
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.financialmanagementservice.exception.ServiceTypeCreationException;
import ru.utlc.financialmanagementservice.mapper.ServiceTypeMapper;
import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;

import java.util.Objects;

import static ru.utlc.financialmanagementservice.constants.CacheNames.SERVICE_TYPES;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceTypeService {
    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceTypeMapper serviceTypeMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = SERVICE_TYPES, key = "'all'")
    public Flux<ServiceTypeReadDto> findAll() {
        return serviceTypeRepository.findAll()
                .map(serviceTypeMapper::toDto)
                .doOnNext(entity -> Objects.requireNonNull(cacheManager.getCache(SERVICE_TYPES)).put(entity.id(), entity));
    }

    @Cacheable(value = SERVICE_TYPES, key = "#p0")
    public Mono<ServiceTypeReadDto> findById(Integer id) {
        return serviceTypeRepository.findById(id)
                .map(serviceTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = SERVICE_TYPES, key = "'all'")
    @CachePut(value = SERVICE_TYPES, key = "#result.id")
    public Mono<ServiceTypeReadDto> create(ServiceTypeCreateUpdateDto dto) {
        return Mono.just(dto)
                .map(serviceTypeMapper::toEntity)
                .flatMap(serviceTypeRepository::save)
                .map(serviceTypeMapper::toDto)
                .onErrorMap(e -> new ServiceTypeCreationException("error.entity.serviceType.creation"));
    }

    @Transactional
    @CacheEvict(value = SERVICE_TYPES, key = "'all'")
    @CachePut(value = SERVICE_TYPES, key = "#result.id")
    public Mono<ServiceTypeReadDto> update(Integer id, ServiceTypeCreateUpdateDto dto) {
        return serviceTypeRepository.findById(id)
                .flatMap(entity -> Mono.just(serviceTypeMapper.update(entity, dto)))
                .flatMap(serviceTypeRepository::save)
                .map(serviceTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = SERVICE_TYPES, allEntries = true) //todo improve by selectively deleting only the cached entity while updating 'all'.
    public Mono<Boolean> delete(Integer id) {
        return serviceTypeRepository.findById(id)
                .flatMap(serviceType -> serviceTypeRepository.delete(serviceType)
                        .thenReturn(true))
                .defaultIfEmpty(false);
    }
}
