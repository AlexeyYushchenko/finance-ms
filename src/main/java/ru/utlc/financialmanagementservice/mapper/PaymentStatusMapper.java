package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.paymentstatus.PaymentStatusCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymentstatus.PaymentStatusReadDto;
import ru.utlc.financialmanagementservice.model.PaymentStatus;

@Mapper
public interface PaymentStatusMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    PaymentStatusReadDto toDto(PaymentStatus entity);  // Entity to DTO

    PaymentStatus toEntity(PaymentStatusCreateUpdateDto dto);  // DTO to Entity

    PaymentStatus update(@MappingTarget PaymentStatus entity, PaymentStatusCreateUpdateDto dto);
}
