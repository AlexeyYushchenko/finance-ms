package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.model.Invoice;

@Mapper(uses = {InvoiceStatusMapper.class, ServiceTypeMapper.class, CurrencyMapper.class})
public interface InvoiceMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    InvoiceReadDto toDto(Invoice invoice);

    Invoice toEntity(InvoiceCreateUpdateDto dto);

    Invoice update(@MappingTarget Invoice invoice, InvoiceCreateUpdateDto dto);
}
