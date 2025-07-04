package ru.utlc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.dto.partnerbalance.PartnerBalanceReportDto;
import ru.utlc.service.PartnerBalanceService;

import java.time.LocalDate;

import static ru.utlc.constants.ApiPaths.CLIENT_BALANCES;
/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@RestController
@RequestMapping(CLIENT_BALANCES)
@RequiredArgsConstructor
public class ClientBalanceController {

    private final PartnerBalanceService partnerBalanceService;

    @GetMapping("/{partnerId}")
    public Mono<PartnerBalanceReportDto> getClientBalance(
            @PathVariable("partnerId") final Long partnerId,
            @RequestParam(name = "reportDate", required = false) final LocalDate reportDate
    ) {
        final LocalDate date = (reportDate != null) ? reportDate : LocalDate.now();
        return partnerBalanceService.getPartnerBalance(partnerId, date);
    }
}
