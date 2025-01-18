package ru.utlc.financialmanagementservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReadDto;
import ru.utlc.financialmanagementservice.service.ClientBalanceService;

import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.CLIENT_BALANCES;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(CLIENT_BALANCES)
public class ClientBalanceRestController {

    private final ClientBalanceService clientBalanceService;

    @GetMapping
    public Mono<ResponseEntity<List<ClientBalanceReadDto>>> findAll() {
        return clientBalanceService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{clientId}")
    public Mono<ResponseEntity<List<ClientBalanceReadDto>>> findByClientId(@PathVariable("clientId") final Integer clientId) {
        return clientBalanceService.findByClientId(clientId)
                .collectList()
                .flatMap(balances -> {
                    if (balances.isEmpty()) {
                        return Mono.just(ResponseEntity.notFound().build());
                    } else {
                        return Mono.just(ResponseEntity.ok(balances));
                    }
                });
    }

    @GetMapping("/{clientId}/{currencyId}")
    public Mono<ResponseEntity<ClientBalanceReadDto>> findByClientIdAndCurrencyId(@PathVariable("clientId") final Integer clientId,
                                                                                  @PathVariable("currencyId") final Integer currencyId) {
        return clientBalanceService.findByClientIdAndCurrencyId(clientId, currencyId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
