package ru.utlc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.response.Response;
import ru.utlc.service.PaymentTypeService;

import java.net.URI;
import java.util.List;

import static ru.utlc.constants.ApiPaths.PAYMENT_TYPES;

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
@RequestMapping(PAYMENT_TYPES)
public class PaymentTypeRestController {

    private final PaymentTypeService paymentTypeService;

    @GetMapping
    public Mono<ResponseEntity<List<PaymentTypeReadDto>>> findAll() {
        return paymentTypeService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentTypeReadDto>> findById(@PathVariable("id") final Integer id) {
        return paymentTypeService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json")
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid PaymentTypeCreateUpdateDto dto) {
        return paymentTypeService.create(dto)
                .map(paymentTypeReadDto -> {
                    URI location = URI.create("/paymentTypes/" + paymentTypeReadDto.id());
                    return ResponseEntity.created(location).body(new Response(paymentTypeReadDto));
                });
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Integer id,
                                                 @RequestBody @Valid PaymentTypeCreateUpdateDto dto) {
        return paymentTypeService.update(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Integer id) {
        return paymentTypeService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}