//package ru.utlc.financialmanagementservice.mapper;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import org.mapstruct.MappingTarget;
//import ru.utlc.financialmanagementservice.dto.partnertype.PartnerTypeCreateUpdateDto;
//import ru.utlc.financialmanagementservice.dto.partnertype.PartnerTypeReadDto;
//import ru.utlc.financialmanagementservice.model.PartnerType;
//
//@Mapper
//public interface PartnerTypeMapper {
//    @Mapping(target = "auditingInfoDto", source = ".")
//    PartnerTypeReadDto toDto(PartnerType partnerType);
//
//    PartnerType toEntity(PartnerTypeCreateUpdateDto createUpdateDto);
//
//    PartnerType update(@MappingTarget PartnerType partnerType, PartnerTypeCreateUpdateDto createUpdateDto);
//}