package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.payment.PaymentCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.AllocationService;
import ru.utlc.financialmanagementservice.service.PaymentService;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static ru.utlc.financialmanagementservice.constants.ApiPaths.PAYMENTS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(PAYMENTS)  // e.g. "/api/payments"
public class PaymentRestController {

    private final PaymentService paymentService;
    private final AllocationService allocationService;

    /*
     * ---------------------------
     * BASIC CRUD
     * ---------------------------
     */

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

    @GetMapping("/partnerId/{partnerId}")
    public Mono<ResponseEntity<List<PaymentReadDto>>> findAllByPartnerId(@PathVariable("partnerId") final Long partnerId) {
        return paymentService.findByPartnerId(partnerId)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid final PaymentCreateUpdateDto dto) {
        return paymentService.create(dto)
                .map(created -> {
                    URI location = URI.create("/payments/" + created.id());
                    return ResponseEntity.created(location).body(new Response(created));
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Response>> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid final PaymentCreateUpdateDto dto
    ) {
        return paymentService.update(id, dto)
                .map(updated -> ResponseEntity.ok(new Response(updated)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Long id) {
        return paymentService.delete(id)
                .map(deleted -> Boolean.TRUE.equals(deleted)
                        ? ResponseEntity.status(NO_CONTENT).build()
                        : ResponseEntity.status(NOT_FOUND).build()
                );
    }
}