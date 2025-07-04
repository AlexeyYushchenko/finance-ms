package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.dto.paymentstatus.PaymentStatusCreateUpdateDto;
import ru.utlc.dto.paymentstatus.PaymentStatusReadDto;
import ru.utlc.model.PaymentStatus;

@Mapper
public interface PaymentStatusMapper {

    @Mapping(target = "auditingInfoDto", source = ".")
    PaymentStatusReadDto toDto(PaymentStatus entity);  // Entity to DTO

    PaymentStatus toEntity(PaymentStatusCreateUpdateDto dto);  // DTO to Entity

    PaymentStatus update(@MappingTarget PaymentStatus entity, PaymentStatusCreateUpdateDto dto);
}
