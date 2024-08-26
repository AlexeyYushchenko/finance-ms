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

//    @GetMapping
//    public Mono<ResponseEntity<List<InvoiceEnrichedReadDto>>> findAllEnriched(@RequestParam("language") String language) {
//        return invoiceService.findAll()
//                .flatMap(invoice -> Mono.zip(
//                                        clientService.findClientById(invoice.clientId(), language),
//                                        serviceTypeService.findById(invoice.serviceTypeId()),
//                                        currencyService.findById(invoice.currencyId()),
//                                        invoiceStatusService.findById(invoice.statusId()),
//                                        Mono.just(invoice)
//                                )
//                                .map(tuple -> new InvoiceEnrichedReadDto(
//                                        invoice.id(),
//                                        tuple.getT1(),  // ClientReadDto
//                                        tuple.getT2(),  // ServiceTypeReadDto
//                                        invoice.totalAmount(),
//                                        tuple.getT3(),  // CurrencyReadDto
//                                        invoice.issueDate(),
//                                        invoice.dueDate(),
//                                        invoice.commentary(),
//                                        invoice.shipmentId(),
//                                        tuple.getT4(),  // InvoiceStatusReadDto
//                                        tuple.getT4().auditingInfoDto()
//                                ))
//                )
//                .collectList()
//                .map(ResponseEntity::ok)
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }
//
//
//    @GetMapping("/{id}")
//    public Mono<ResponseEntity<InvoiceEnrichedReadDto>> findById(@PathVariable("id") final Long id, @RequestParam("language") String language) {
//        return invoiceService.findById(id)
//                .flatMap(invoice -> Mono.zip(
//                                        clientService.findClientById(invoice.clientId(), language),
//                                        serviceTypeService.findById(invoice.serviceTypeId()),
//                                        currencyService.findById(invoice.currencyId()),
//                                        invoiceStatusService.findById(invoice.statusId()),
//                                        Mono.just(invoice)
//                                )
//                                .map(tuple -> new InvoiceEnrichedReadDto(
//                                        invoice.id(),
//                                        tuple.getT1(),  // ClientReadDto
//                                        tuple.getT2(),  // ServiceTypeReadDto
//                                        invoice.totalAmount(),
//                                        tuple.getT3(),  // CurrencyReadDto
//                                        invoice.issueDate(),
//                                        invoice.dueDate(),
//                                        invoice.commentary(),
//                                        invoice.shipmentId(),
//                                        tuple.getT4(),  // InvoiceStatusReadDto
//                                        tuple.getT4().auditingInfoDto()
//                                ))
//                )
//                .map(ResponseEntity::ok)
//                .defaultIfEmpty(ResponseEntity.notFound().build());
//    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid final InvoiceCreateUpdateDto dto,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            return ValidationErrorUtil.handleValidationErrors(bindingResult);
        }

        return invoiceService.create(dto)
                .map(invoiceReadDto -> {
                    final URI location = URI.create("/invoices/" + invoiceReadDto.id());
                    return ResponseEntity.created(location).body(new Response(invoiceReadDto.id()));
                });
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Long id,
                                                 @RequestBody @Valid InvoiceCreateUpdateDto dto,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            return ValidationErrorUtil.handleValidationErrors(bindingResult);
        }

        return invoiceService.update(id, dto)
                .map(updatedDto -> new ResponseEntity<>(new Response(updatedDto), HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Long id) {
        return invoiceService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}
