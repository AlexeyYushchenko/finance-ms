package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.paymentstatus.PaymentStatusCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymentstatus.PaymentStatusReadDto;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.PaymentStatusService;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/paymentStatuses")  // or wherever you prefer
public class PaymentStatusRestController {

    private final PaymentStatusService paymentStatusService;

    @GetMapping
    public Mono<ResponseEntity<List<PaymentStatusReadDto>>> findAll() {
        return paymentStatusService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentStatusReadDto>> findById(@PathVariable("id") final Integer id) {
        return paymentStatusService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json")
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid PaymentStatusCreateUpdateDto dto) {
        return paymentStatusService.create(dto)
                .map(paymentStatusReadDto -> {
                    URI location = URI.create("/paymentStatuses/" + paymentStatusReadDto.id());
                    return ResponseEntity.created(location).body(new Response(paymentStatusReadDto));
                });
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Integer id,
                                                 @RequestBody @Valid PaymentStatusCreateUpdateDto dto) {
        return paymentStatusService.update(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Integer id) {
        return paymentStatusService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}
