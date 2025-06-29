package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.InvoiceService;
import ru.utlc.financialmanagementservice.service.TransactionLedgerService;

import java.net.URI;
import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.INVOICES;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(INVOICES)
public class InvoiceRestController {

    private final InvoiceService invoiceService;

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

    @GetMapping("/partner/{partnerId}")
    public Mono<ResponseEntity<List<InvoiceReadDto>>> getInvoicesByPartnerId(@PathVariable("partnerId") Long partnerId) {
        return invoiceService.findByPartnerId(partnerId)
                .collectList()
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
    public Mono<ResponseEntity<Response>> update(
            @PathVariable("id") final Long id,
            @RequestBody @Valid InvoiceCreateUpdateDto dto
    ) {
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
