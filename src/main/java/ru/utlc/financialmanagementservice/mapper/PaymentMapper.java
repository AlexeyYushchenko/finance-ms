package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.model.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "auditingInfoDto", source = ".") // Map auditing info from Payment entity to DTO
    @Mapping(target = "isFullyAllocated", expression = "java(payment.isFullyAllocated())")
    PaymentReadDto toDto(Payment payment);

    Payment toEntity(PaymentCreateUpdateDto dto);
    Payment toEntity(PaymentReadDto dto);

    Payment update(@MappingTarget Payment payment, PaymentCreateUpdateDto dto);
}

