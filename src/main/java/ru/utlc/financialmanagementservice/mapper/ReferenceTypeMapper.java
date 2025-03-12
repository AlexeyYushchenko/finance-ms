package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.financialmanagementservice.model.PaymentType;

@Mapper
public interface PaymentTypeMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    PaymentTypeReadDto toDto(PaymentType paymentType);  // Entity to DTO

    PaymentType toEntity(PaymentTypeCreateUpdateDto createUpdateDto);  // DTO to Entity

    PaymentType update(@MappingTarget PaymentType paymentType, PaymentTypeCreateUpdateDto createUpdateDto);
}