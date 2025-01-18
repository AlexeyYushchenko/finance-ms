package ru.utlc.financialmanagementservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReadDto;
import ru.utlc.financialmanagementservice.model.ClientBalance;

@Mapper()
public interface ClientBalanceMapper {
    @Mapping(target = "auditingInfoDto", source = ".")
    ClientBalanceReadDto toDto(ClientBalance clientBalance);
}

