package ru.utlc.financialmanagementservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.client.ClientReadDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceEnrichedReadDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.integration.ClientService;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.InvoiceService;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.INVOICES;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(INVOICES)
public class InvoiceRestController {

    private final InvoiceService invoiceService;
    private final ClientService clientService;

    @GetMapping
    public Mono<ResponseEntity<List<InvoiceEnrichedReadDto>>> findAll(@RequestParam(name = "language") String language) {
        return invoiceService.findAll()
                .flatMap(invoiceDto -> clientService.findClientById(invoiceDto.clientId(), language)
                        .map(clientReadDto -> new InvoiceEnrichedReadDto(
                                invoiceDto.id(),
                                clientReadDto,
                                invoiceDto.serviceType(),
                                invoiceDto.totalAmount(),
                                invoiceDto.currency(),
                                invoiceDto.issueDate(),
                                invoiceDto.dueDate(),
                                invoiceDto.commentary(),
                                invoiceDto.shipmentId(),
                                invoiceDto.invoiceStatus(),
                                invoiceDto.auditingInfoDto()
                        ))
                        .defaultIfEmpty(new InvoiceEnrichedReadDto(
                                invoiceDto.id(),
                                null,
                                invoiceDto.serviceType(),
                                invoiceDto.totalAmount(),
                                invoiceDto.currency(),
                                invoiceDto.issueDate(),
                                invoiceDto.dueDate(),
                                invoiceDto.commentary(),
                                invoiceDto.shipmentId(),
                                invoiceDto.invoiceStatus(),
                                invoiceDto.auditingInfoDto()
                        ))
                        .onErrorResume(e -> {
                            log.error("Error fetching client for invoice id {}: {}", invoiceDto.id(), e.getMessage());
                            return Mono.just(new InvoiceEnrichedReadDto(
                                    invoiceDto.id(),
                                    null,
                                    invoiceDto.serviceType(),
                                    invoiceDto.totalAmount(),
                                    invoiceDto.currency(),
                                    invoiceDto.issueDate(),
                                    invoiceDto.dueDate(),
                                    invoiceDto.commentary(),
                                    invoiceDto.shipmentId(),
                                    invoiceDto.invoiceStatus(),
                                    invoiceDto.auditingInfoDto()
                            ));
                        })
                )
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<InvoiceEnrichedReadDto>> findById(@PathVariable("id") final Long id, @RequestParam(name = "language") String language) {

        return invoiceService.findById(id)
                .map(invoiceDto -> clientService.findClientById(invoiceDto.clientId(), language)
                        .map(clientReadDto -> new InvoiceEnrichedReadDto(
                                invoiceDto.id(),
                                clientReadDto,
                                invoiceDto.serviceType(),
                                invoiceDto.totalAmount(),
                                invoiceDto.currency(),
                                invoiceDto.issueDate(),
                                invoiceDto.dueDate(),
                                invoiceDto.commentary(),
                                invoiceDto.shipmentId(),
                                invoiceDto.invoiceStatus(),
                                invoiceDto.auditingInfoDto()
                        ))
                        .defaultIfEmpty(new InvoiceEnrichedReadDto(
                                invoiceDto.id(),
                                null,
                                invoiceDto.serviceType(),
                                invoiceDto.totalAmount(),
                                invoiceDto.currency(),
                                invoiceDto.issueDate(),
                                invoiceDto.dueDate(),
                                invoiceDto.commentary(),
                                invoiceDto.shipmentId(),
                                invoiceDto.invoiceStatus(),
                                invoiceDto.auditingInfoDto()
                        ))
                )
                .map(ResponseEntity::ok)
                .doOnError(ResponseEntity::notFound);


                .map(clientReadDto -> new InvoiceEnrichedReadDto(
                        invoiceDto.id(),
                        clientReadDto,
                        invoiceDto.serviceType(),
                        invoiceDto.totalAmount(),
                        invoiceDto.currency(),
                        invoiceDto.issueDate(),
                        invoiceDto.dueDate(),
                        invoiceDto.commentary(),
                        invoiceDto.shipmentId(),
                        invoiceDto.invoiceStatus(),
                        invoiceDto.auditingInfoDto()
                ))
                .defaultIfEmpty(new InvoiceEnrichedReadDto(
                        invoiceDto.id(),
                        null,
                        invoiceDto.serviceType(),
                        invoiceDto.totalAmount(),
                        invoiceDto.currency(),
                        invoiceDto.issueDate(),
                        invoiceDto.dueDate(),
                        invoiceDto.commentary(),
                        invoiceDto.shipmentId(),
                        invoiceDto.invoiceStatus(),
                        invoiceDto.auditingInfoDto()
                ))

        if (invoiceDtoOpt.isPresent()) {
            InvoiceReadDto invoiceDto = invoiceDtoOpt.get();
            InvoiceEnrichedReadDto enrichedInvoice = clientService.findClientById(invoiceDto.clientId(), language)
                    .map(clientReadDto -> new InvoiceEnrichedReadDto(
                            invoiceDto.id(),
                            clientReadDto,
                            invoiceDto.serviceType(),
                            invoiceDto.totalAmount(),
                            invoiceDto.currency(),
                            invoiceDto.issueDate(),
                            invoiceDto.dueDate(),
                            invoiceDto.commentary(),
                            invoiceDto.shipmentId(),
                            invoiceDto.invoiceStatus(),
                            invoiceDto.auditingInfoDto()
                    ))
                    .defaultIfEmpty(new InvoiceEnrichedReadDto(
                            invoiceDto.id(),
                            null,
                            invoiceDto.serviceType(),
                            invoiceDto.totalAmount(),
                            invoiceDto.currency(),
                            invoiceDto.issueDate(),
                            invoiceDto.dueDate(),
                            invoiceDto.commentary(),
                            invoiceDto.shipmentId(),
                            invoiceDto.invoiceStatus(),
                            invoiceDto.auditingInfoDto()
                    ))
                    .onErrorResume(e -> {
                        log.error("Error fetching client for invoice id {}: {}", invoiceDto.id(), e.getMessage());
                        return Mono.just(new InvoiceEnrichedReadDto(
                                invoiceDto.id(),
                                null,
                                invoiceDto.serviceType(),
                                invoiceDto.totalAmount(),
                                invoiceDto.currency(),
                                invoiceDto.issueDate(),
                                invoiceDto.dueDate(),
                                invoiceDto.commentary(),
                                invoiceDto.shipmentId(),
                                invoiceDto.invoiceStatus(),
                                invoiceDto.auditingInfoDto()
                        ));
                    }).block();

            return ResponseEntity.ok(enrichedInvoice);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> create(@RequestBody @Validated final InvoiceCreateUpdateDto dto,
                                           final BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            return handleValidationErrors(bindingResult);
        }

        final var invoiceReadDto = invoiceService.create(dto);
        final URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(invoiceReadDto.id())
                .toUri();

        return ResponseEntity.created(location).body(new Response(invoiceReadDto));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/{id}")
    public ResponseEntity<Response> update(@PathVariable("id") final Long id,
                                           @RequestBody @Validated final InvoiceCreateUpdateDto dto,
                                           final BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            return handleValidationErrors(bindingResult);
        }

        return invoiceService.update(id, dto)
                .map(updatedDto -> new ResponseEntity<>(new Response(updatedDto), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Long id) {
        return invoiceService.delete(id)
                .flatMap(deleted -> deleted
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                );
    }

    private ResponseEntity<Response> handleValidationErrors(final BindingResult bindingResult) {
        final List<String> errorMessages = bindingResult.getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        final Response response = new Response(errorMessages, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
