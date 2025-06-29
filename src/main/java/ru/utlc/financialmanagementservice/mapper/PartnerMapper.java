//package ru.utlc.financialmanagementservice.mapper;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import org.mapstruct.MappingTarget;
//import ru.utlc.financialmanagementservice.dto.partner.PartnerCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.partner.PartnerReadDto;
//import ru.utlc.financialmanagementservice.model.Partner;
//
///**
// * Maps between Partner entity and Partner DTOs.
// */
//@Mapper
//public interface PartnerMapper {
//
//    @Mapping(target = "auditingInfoDto", source = ".")
//    PartnerReadDto toDto(Partner partner);
//
//    Partner toEntity(PartnerCreateUpdateDto dto);
//
//    Partner update(@MappingTarget Partner partner, PartnerCreateUpdateDto dto);
//}
