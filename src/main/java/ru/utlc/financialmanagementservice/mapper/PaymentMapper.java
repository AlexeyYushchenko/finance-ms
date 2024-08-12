//package ru.utlc.financialmanagementservice.mapper;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import org.mapstruct.MappingTarget;
//import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
//import ru.utlc.financialmanagementservice.model.Payment;
//
//@Mapper
//public interface PaymentMapper {
//
//    @Mapping(target = "auditingInfoDto", source = ".")
//    PaymentReadDto toDto(Payment payment);
//
//    Payment toEntity(PaymentCreateUpdateDto dto);
//
//    Payment update(@MappingTarget Payment payment, PaymentCreateUpdateDto dto);
//}
