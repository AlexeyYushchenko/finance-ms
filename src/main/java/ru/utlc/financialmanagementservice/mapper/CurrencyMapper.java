package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.model.Currency;

@Mapper
public interface CurrencyMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    CurrencyReadDto toDto(Currency currency);

    Currency toEntity(CurrencyCreateUpdateDto dto);

    Currency update(@MappingTarget Currency currency, CurrencyCreateUpdateDto dto);
}
