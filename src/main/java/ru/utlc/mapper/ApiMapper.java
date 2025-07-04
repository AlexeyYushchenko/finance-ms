package ru.utlc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.utlc.dto.currency.CurrencyReadDto;
import ru.utlc.dto.invoice.InvoiceReadDto;
import ru.utlc.dto.invoicestatus.InvoiceStatusReadDto;
import ru.utlc.dto.ledgerentry.LedgerEntryReadDto;
import ru.utlc.dto.payment.PaymentReadDto;
import ru.utlc.dto.paymentstatus.PaymentStatusReadDto;
import ru.utlc.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.dto.referencetype.ReferenceTypeReadDto;
import ru.utlc.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.finance.api.dto.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ApiMapper {

    /*──────────────── single objects ────────────────*/

    CurrencyDto toApi(CurrencyReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    InvoiceDto toApi(InvoiceReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    InvoiceStatusDto toApi(InvoiceStatusReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    LedgerEntryDto toApi(LedgerEntryReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    PaymentDto toApi(PaymentReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    PaymentStatusDto toApi(PaymentStatusReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    PaymentTypeDto toApi(PaymentTypeReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    ReferenceTypeDto toApi(ReferenceTypeReadDto src);

    @Mapping(source = "auditingInfoDto.createdAt",  target = "createdAt")
    @Mapping(source = "auditingInfoDto.modifiedAt", target = "modifiedAt")
    ServiceTypeDto toApi(ServiceTypeReadDto src);

    /*──────────────── enum hop ────────────────*/
    default InvoiceDirection map(ru.utlc.model.InvoiceDirection dir) {
        return dir == null ? null : InvoiceDirection.valueOf(dir.name());
    }
}
