package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.dto.currency.CurrencyReadDto;
import ru.utlc.model.Currency;

@Mapper
public interface CurrencyMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    CurrencyReadDto toDto(Currency currency);

    Currency toEntity(CurrencyCreateUpdateDto dto);

    Currency update(@MappingTarget Currency currency, CurrencyCreateUpdateDto dto);
}
