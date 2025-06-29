package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.financialmanagementservice.dto.referencetype.ReferenceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.referencetype.ReferenceTypeReadDto;
import ru.utlc.financialmanagementservice.model.PaymentType;
import ru.utlc.financialmanagementservice.model.ReferenceType;

@Mapper
public interface ReferenceTypeMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    ReferenceTypeReadDto toDto(ReferenceType referenceType);

    ReferenceType toEntity(ReferenceTypeCreateUpdateDto createUpdateDto);

    ReferenceType update(@MappingTarget ReferenceType referenceType, ReferenceTypeCreateUpdateDto createUpdateDto);
}