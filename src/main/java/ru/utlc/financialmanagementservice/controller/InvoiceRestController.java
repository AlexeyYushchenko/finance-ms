package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.integration.ClientService;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.CurrencyService;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.InvoiceStatusService;
import ru.utlc.financialmanagementservice.service.ServiceTypeService;
import ru.utlc.financialmanagementservice.util.ValidationErrorUtil;

import java.net.URI;
import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.INVOICES;

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
@RequestMapping(INVOICES)
public class InvoiceRestController {

    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final CurrencyService currencyService;
    private final ServiceTypeService serviceTypeService;
    private final InvoiceStatusService invoiceStatusService;

    @GetMapping
    public Mono<ResponseEntity<List<InvoiceReadDto>>> findAll() {
        return invoiceService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<InvoiceReadDto>> findById(@PathVariable("id") final Long id) {
        return invoiceService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid final InvoiceCreateUpdateDto dto) {
        return invoiceService.create(dto)
                .map(invoiceReadDto -> {
                    final URI location = URI.create("/invoices/" + invoiceReadDto.id());
                    return ResponseEntity.created(location).body(new Response(invoiceReadDto));
                });
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Long id,
                                                 @RequestBody @Valid InvoiceCreateUpdateDto dto) {
        return invoiceService.update(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Long id) {
        return invoiceService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(ResponseEntity.noContent().build())
                        : Mono.just(ResponseEntity.notFound().build()));
    }
}
