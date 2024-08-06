package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.exception.InvoiceCreationException;
import ru.utlc.financialmanagementservice.mapper.InvoiceMapper;
import ru.utlc.financialmanagementservice.model.Invoice;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;
import ru.utlc.financialmanagementservice.repository.InvoiceRepository;
import ru.utlc.financialmanagementservice.repository.InvoiceStatusRepository;
import ru.utlc.financialmanagementservice.repository.ServiceTypeRepository;

import java.util.List;
import java.util.Optional;

import static ru.utlc.financialmanagementservice.constants.CacheNames.INVOICES;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceStatusRepository invoiceStatusRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CurrencyRepository currencyRepository;
    private final CacheManager cacheManager;

    @Cacheable(value = INVOICES, key = "'all'")
    public List<InvoiceReadDto> findAll() {
        List<InvoiceReadDto> list = invoiceRepository.findAll().stream()
                .map(invoiceMapper::toDto)
                .toList();

        list.forEach(entity -> cacheManager.getCache(INVOICES).put(entity.id(), entity));
        return list;
    }

    @Cacheable(value = INVOICES, key = "#p0")
    public Optional<InvoiceReadDto> findById(Long id) {
        return invoiceRepository.findById(id).map(invoiceMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICES, allEntries = true)
    @CachePut(value = INVOICES, key = "#result.id")
    public InvoiceReadDto create(InvoiceCreateUpdateDto dto) throws InvoiceCreationException {
        return Optional.of(dto)
                .map(invoiceMapper::toEntity)
                .map(entity -> setUpOtherEntitiesToMainEntity(entity, dto))
                .map(invoiceRepository::save)
                .map(invoiceMapper::toDto)
                .orElseThrow(() -> new InvoiceCreationException("error.entity.invoice.creation"));
    }

    @Transactional
    @CacheEvict(value = INVOICES, allEntries = true)
    @CachePut(value = INVOICES, key = "#result.id")
    public Optional<InvoiceReadDto> update(Long id, InvoiceCreateUpdateDto dto) {
        return invoiceRepository.findById(id)
                .map(entity -> invoiceMapper.update(entity, dto))
                .map(entity -> setUpOtherEntitiesToMainEntity(entity, dto))
                .map(invoiceRepository::saveAndFlush)
                .map(invoiceMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = INVOICES, allEntries = true)
    public boolean delete(Long id) {
        return invoiceRepository.findById(id)
                .map(invoice -> {
                    invoiceRepository.delete(invoice);
                    invoiceRepository.flush();
                    return true;
                })
                .orElse(false);
    }

    private Invoice setUpOtherEntitiesToMainEntity(Invoice entity, InvoiceCreateUpdateDto dto) {
        var invoiceStatus = Optional.ofNullable(dto.invoiceStatusId())
                .flatMap(invoiceStatusRepository::findById)
                .orElse(null);
        entity.setInvoiceStatus(invoiceStatus);
        var currency = Optional.ofNullable(dto.currencyId())
                .flatMap(currencyRepository::findById)
                .orElse(null);
        entity.setCurrency(currency);
        var serviceType = Optional.ofNullable(dto.serviceTypeId())
                .flatMap(serviceTypeRepository::findById)
                .orElse(null);
        entity.setServiceType(serviceType);

        return entity;
    }
}