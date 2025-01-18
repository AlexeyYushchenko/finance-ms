package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.clientbalance.ClientBalanceReadDto;
import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceCreateDto;
import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.paymentinvoice.PaymentInvoiceUpdateDto;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.PaymentInvoiceService;

import java.net.URI;
import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.PAYMENT_INVOICES;

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
@RequestMapping(PAYMENT_INVOICES)
public class PaymentInvoiceRestController {

    private final PaymentInvoiceService paymentInvoiceService;

    @GetMapping
    public Mono<ResponseEntity<List<PaymentInvoiceReadDto>>> findAll() {
        return paymentInvoiceService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentInvoiceReadDto>> findById(@PathVariable("id") final Long id) {
        return paymentInvoiceService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/clientId/{clientId}")
    public Mono<ResponseEntity<List<PaymentInvoiceReadDto>>> findAllByClientId(@PathVariable("clientId") final Integer clientId) {
        return paymentInvoiceService.findAllByClientId(clientId)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/paymentId/{paymentId}")
    public Mono<ResponseEntity<List<PaymentInvoiceReadDto>>> findAllByPaymentId(@PathVariable("paymentId") final Long paymentId) {
        return paymentInvoiceService.findAllByPaymentId(paymentId)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/invoiceId/{invoiceId}")
    public Mono<ResponseEntity<List<PaymentInvoiceReadDto>>> findAllByInvoiceId(@PathVariable("invoiceId") final Long invoiceId) {
        return paymentInvoiceService.findAllByInvoiceId(invoiceId)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid final PaymentInvoiceCreateDto dto) {
        return paymentInvoiceService.allocatePaymentToInvoice(dto)
                .map(readDto -> {
                    final URI location = URI.create("/payment-invoices/" + readDto.id());
                    return ResponseEntity.created(location).body(new Response(readDto));
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Long id,
                                                 @RequestBody @Valid final PaymentInvoiceUpdateDto dto) {
        return paymentInvoiceService.updateAllocation(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Long id) {
        return paymentInvoiceService.deallocatePaymentFromInvoice(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
