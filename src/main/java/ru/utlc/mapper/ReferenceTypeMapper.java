package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.utlc.dto.referencetype.ReferenceTypeCreateUpdateDto;
import ru.utlc.dto.referencetype.ReferenceTypeReadDto;
import ru.utlc.model.ReferenceType;

@Mapper
public interface ReferenceTypeMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    ReferenceTypeReadDto toDto(ReferenceType referenceType);

    ReferenceType toEntity(ReferenceTypeCreateUpdateDto createUpdateDto);

    ReferenceType update(@MappingTarget ReferenceType referenceType, ReferenceTypeCreateUpdateDto createUpdateDto);
}