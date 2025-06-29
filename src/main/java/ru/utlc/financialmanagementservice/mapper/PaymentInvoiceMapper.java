//package ru.utlc.financialmanagementservice.mapper;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import org.mapstruct.MappingTarget;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceCreateDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceReadDto;
//import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceUpdateDto;
//import ru.utlc.financialmanagementservice.model.PaymentInvoice;
//
//@Mapper()
//public interface PaymentInvoiceMapper {
//
//    @Mapping(target = "auditingInfoDto", source = ".")
//    PaymentInvoiceReadDto toDto(PaymentInvoice paymentInvoice);
//
//    PaymentInvoice toEntity(PaymentInvoiceCreateUpdateDto dto);
//
//    PaymentInvoiceCreateUpdateDto toCreateUpdateDto(PaymentInvoiceCreateDto createDto);
//    @Mapping(target = "allocatedAmount", source = "dto.allocatedAmount")
//    PaymentInvoiceCreateUpdateDto toCreateUpdateDto(PaymentInvoiceUpdateDto dto, PaymentInvoice paymentInvoice);
//
//    PaymentInvoice update(@MappingTarget PaymentInvoice paymentInvoice, PaymentInvoiceCreateUpdateDto dto);
//}