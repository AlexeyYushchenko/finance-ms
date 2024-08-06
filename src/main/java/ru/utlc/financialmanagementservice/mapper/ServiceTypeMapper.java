package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.financialmanagementservice.model.ServiceType;

@Mapper
public interface ServiceTypeMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    ServiceTypeReadDto toDto(ServiceType serviceType);  // Entity to DTO

    ServiceType toEntity(ServiceTypeCreateUpdateDto createUpdateDto);  // DTO to Entity

    ServiceType update(@MappingTarget ServiceType serviceType, ServiceTypeCreateUpdateDto createUpdateDto);
}