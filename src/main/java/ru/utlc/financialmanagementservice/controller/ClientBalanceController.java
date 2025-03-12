package ru.utlc.financialmanagementservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReportDto;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;

import java.time.LocalDate;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.CLIENT_BALANCES;

@RestController
@RequestMapping(CLIENT_BALANCES)
@RequiredArgsConstructor
public class ClientBalanceController {

    private final ClientBalanceService clientBalanceService;

    @GetMapping("/{partnerId}")
    public Mono<ClientBalanceReportDto> getClientBalance(
            @PathVariable("partnerId") final Long partnerId,
            @RequestParam(name = "reportDate", required = false) final LocalDate reportDate
    ) {
        final LocalDate date = (reportDate != null) ? reportDate : LocalDate.now();
        return clientBalanceService.getClientBalance(partnerId, date);
    }
}
