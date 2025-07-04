package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.dto.invoicestatus.InvoiceStatusCreateUpdateDto;
import ru.utlc.dto.invoicestatus.InvoiceStatusReadDto;
import ru.utlc.model.InvoiceStatus;

@Mapper
public interface InvoiceStatusMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    InvoiceStatusReadDto toDto(InvoiceStatus entity);  // Entity to DTO
    InvoiceStatus toEntity(InvoiceStatusCreateUpdateDto dto);  // DTO to Entity
    InvoiceStatus update(@MappingTarget InvoiceStatus entity, InvoiceStatusCreateUpdateDto dto);
}