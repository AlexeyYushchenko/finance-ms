package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.*;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryReadDto;
import ru.utlc.financialmanagementservice.model.LedgerEntry;

@Mapper(componentModel = "spring")
public interface LedgerEntryMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    LedgerEntryReadDto toReadDto(LedgerEntry ledgerEntry);

    // Convert CreateUpdateDto -> Entity
    LedgerEntry toEntity(LedgerEntryCreateUpdateDto dto);

    LedgerEntry update(@MappingTarget LedgerEntry ledgerEntry, LedgerEntryCreateUpdateDto dto);
}
