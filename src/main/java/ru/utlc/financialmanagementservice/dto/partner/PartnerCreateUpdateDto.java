//package ru.utlc.financialmanagementservice.dto.partner;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Positive;
//
//public record PartnerCreateUpdateDto(
//        @NotNull(message = "{validation.partner.partnerTypeId.required}")
//        Integer partnerTypeId,
//
//        @NotNull(message = "{validation.partner.externalId.required}")
//        @Positive(message = "{validation.partner.externalId.positive}")
//        Long externalId
//) {
//}
