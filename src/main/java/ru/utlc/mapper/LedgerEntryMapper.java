package ru.utlc.mapper;

import org.mapstruct.*;
import ru.utlc.dto.ledgerentry.LedgerEntryCreateUpdateDto;
import ru.utlc.dto.ledgerentry.LedgerEntryReadDto;
import ru.utlc.model.LedgerEntry;

@Mapper(componentModel = "spring")
public interface LedgerEntryMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    LedgerEntryReadDto toReadDto(LedgerEntry ledgerEntry);

    // Convert CreateUpdateDto -> Entity
    LedgerEntry toEntity(LedgerEntryCreateUpdateDto dto);

    LedgerEntry update(@MappingTarget LedgerEntry ledgerEntry, LedgerEntryCreateUpdateDto dto);
}
