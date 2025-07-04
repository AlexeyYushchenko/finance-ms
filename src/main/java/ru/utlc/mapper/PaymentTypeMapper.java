package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.model.PaymentType;

@Mapper
public interface PaymentTypeMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    PaymentTypeReadDto toDto(PaymentType paymentType);  // Entity to DTO

    PaymentType toEntity(PaymentTypeCreateUpdateDto createUpdateDto);  // DTO to Entity

    PaymentType update(@MappingTarget PaymentType paymentType, PaymentTypeCreateUpdateDto createUpdateDto);
}