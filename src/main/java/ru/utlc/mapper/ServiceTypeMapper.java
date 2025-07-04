package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.model.ServiceType;

@Mapper
public interface ServiceTypeMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    ServiceTypeReadDto toDto(ServiceType serviceType);  // Entity to DTO

    ServiceType toEntity(ServiceTypeCreateUpdateDto createUpdateDto);  // DTO to Entity

    ServiceType update(@MappingTarget ServiceType serviceType, ServiceTypeCreateUpdateDto createUpdateDto);
}