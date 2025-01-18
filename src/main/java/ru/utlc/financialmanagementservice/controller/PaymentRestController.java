package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.PaymentService;
import ru.utlc.financialmanagementservice.util.ValidationErrorUtil;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.PAYMENTS;

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
@RequestMapping(PAYMENTS)
public class PaymentRestController {

    private final PaymentService paymentService;

    @GetMapping
    public Mono<ResponseEntity<List<PaymentReadDto>>> findAll() {
        return paymentService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentReadDto>> findById(@PathVariable("id") final Long id) {
        return paymentService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid final PaymentCreateUpdateDto dto) {
        System.out.println(LocalDate.now());
        System.out.println(dto.paymentDate());
        return paymentService.create(dto)
                .map(paymentReadDto -> {
                    final URI location = URI.create("/payments/" + paymentReadDto.id());
                    return ResponseEntity.created(location).body(new Response(paymentReadDto));
                });
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Long id,
                                                 @RequestBody @Valid PaymentCreateUpdateDto dto) {
        return paymentService.update(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Long id) {
        return paymentService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}
