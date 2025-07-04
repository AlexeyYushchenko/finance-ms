package ru.utlc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.dto.currency.CurrencyReadDto;
import ru.utlc.response.Response;
import ru.utlc.service.CurrencyService;

import java.net.URI;
import java.util.List;

import static ru.utlc.constants.ApiPaths.CURRENCIES;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(CURRENCIES)
public class CurrencyRestController {

    private final CurrencyService currencyService;

    @GetMapping
    public Mono<ResponseEntity<List<CurrencyReadDto>>> findAll() {
        return currencyService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CurrencyReadDto>> findById(@PathVariable("id") final Integer id) {
        return currencyService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json")
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid final CurrencyCreateUpdateDto dto) {
        return currencyService.create(dto)
                .map(currencyReadDto -> {
                    URI location = URI.create("/currencies/" + currencyReadDto.id());
                    return ResponseEntity.created(location).body(new Response(currencyReadDto));
                });
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Integer id,
                                                 @RequestBody @Valid final CurrencyCreateUpdateDto dto) {
        return currencyService.update(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Integer id) {
        return currencyService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}