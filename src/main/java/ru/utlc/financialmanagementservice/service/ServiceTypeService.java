package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.financialmanagementservice.exception.ServiceTypeCreationException;
import ru.utlc.financialmanagementservice.mapper.ServiceTypeMapper;
import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;

import java.util.List;
import java.util.Optional;

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
    public List<ServiceTypeReadDto> findAll() {
        List<ServiceTypeReadDto> list = serviceTypeRepository.findAll().stream()
                .map(serviceTypeMapper::toDto)
                .toList();

        list.forEach(entity -> cacheManager.getCache(SERVICE_TYPES).put(entity.id(), entity));
        return list;
    }

    @Cacheable(value = SERVICE_TYPES, key = "#p0")
    public Optional<ServiceTypeReadDto> findById(Integer id) {
        return serviceTypeRepository.findById(id).map(serviceTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = SERVICE_TYPES, allEntries = true)
    @CachePut(value = SERVICE_TYPES, key = "#result.id")
    public ServiceTypeReadDto create(ServiceTypeCreateUpdateDto createUpdateDto) throws ServiceTypeCreationException {
        return Optional.of(createUpdateDto)
                .map(serviceTypeMapper::toEntity)
                .map(serviceTypeRepository::save)
                .map(serviceTypeMapper::toDto)
                .orElseThrow(() -> new ServiceTypeCreationException("error.entity.serviceType.creation"));
    }

    @Transactional
    @CacheEvict(value = SERVICE_TYPES, allEntries = true)
    @CachePut(value = SERVICE_TYPES, key = "#result.id")
    public Optional<ServiceTypeReadDto> update(Integer id, ServiceTypeCreateUpdateDto dto) {
        return serviceTypeRepository.findById(id)
                .map(entity -> serviceTypeMapper.update(entity, dto))
                .map(serviceTypeRepository::saveAndFlush)
                .map(serviceTypeMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = SERVICE_TYPES, allEntries = true)
    public boolean delete(Integer id) {
        return serviceTypeRepository.findById(id)
                .map(serviceType -> {
                    serviceTypeRepository.delete(serviceType);
                    serviceTypeRepository.flush();
                    return true;
                })
                .orElse(false);
    }
}
