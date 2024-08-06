package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.utlc.financialmanagementservice.dto.invoicestatus.InvoiceStatusCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoicestatus.InvoiceStatusReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceStatusCreationException;
import ru.utlc.financialmanagementservice.mapper.InvoiceStatusMapper;
import ru.utlc.financialmanagementservice.repository.InvoiceStatusRepository;

import java.util.List;
import java.util.Optional;

import static ru.utlc.financialmanagementservice.constants.CacheNames.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceStatusService {
    private final InvoiceStatusRepository invoiceStatusRepository;
    private final InvoiceStatusMapper invoiceStatusMapper;
    private final CacheManager cacheManager;

    @Cacheable(value = INVOICE_STATUSES, key = "'all'")
    public List<InvoiceStatusReadDto> findAll() {
        List<InvoiceStatusReadDto> list = invoiceStatusRepository.findAll().stream()
                .map(invoiceStatusMapper::toDto)
                .toList();

        list.forEach(entity -> cacheManager.getCache(INVOICE_STATUSES).put(entity.id(), entity));
        return list;
    }

    @Cacheable(value = INVOICE_STATUSES, key = "#p0")
    public Optional<InvoiceStatusReadDto> findById(Integer id) {
        return invoiceStatusRepository.findById(id).map(invoiceStatusMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICE_STATUSES, allEntries = true)
    @CachePut(value = INVOICE_STATUSES, key = "#result.id")
    public InvoiceStatusReadDto create(InvoiceStatusCreateUpdateDto createUpdateDto) throws InvoiceStatusCreationException {
        return Optional.of(createUpdateDto)
                .map(invoiceStatusMapper::toEntity)
                .map(invoiceStatusRepository::save)
                .map(invoiceStatusMapper::toDto)
                .orElseThrow(() -> new InvoiceStatusCreationException("error.entity.invoiceStatus.creation"));
    }

    @Transactional
    @CacheEvict(value = INVOICE_STATUSES, allEntries = true)
    @CachePut(value = INVOICE_STATUSES, key = "#result.id")
    public Optional<InvoiceStatusReadDto> update(Integer id, InvoiceStatusCreateUpdateDto dto) {
        return invoiceStatusRepository.findById(id)
                .map(entity -> invoiceStatusMapper.update(entity, dto))
                .map(invoiceStatusRepository::saveAndFlush)
                .map(invoiceStatusMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICE_STATUSES, allEntries = true)
    public boolean delete(Integer id) {
        return invoiceStatusRepository.findById(id)
                .map(invoiceStatus -> {
                    invoiceStatusRepository.delete(invoiceStatus);
                    invoiceStatusRepository.flush();
                    return true;
                })
                .orElse(false);
    }
}
