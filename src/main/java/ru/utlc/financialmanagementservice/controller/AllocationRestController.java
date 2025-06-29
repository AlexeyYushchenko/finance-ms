package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.ledgerentry.LedgerEntryReadDto;
import ru.utlc.financialmanagementservice.dto.paymentallocation.PaymentAllocationCreateUpdateDto;
import ru.utlc.financialmanagementservice.service.AllocationService;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.ALLOCATIONS;

@RestController
@RequestMapping(ALLOCATIONS)
@RequiredArgsConstructor
@Slf4j
public class AllocationRestController {

    private final AllocationService allocationService;

    @GetMapping("/client/{clientId}")
    public Flux<LedgerEntryReadDto> findByClientId(@PathVariable("clientId") final Long clientId) {
        return allocationService.findByClientId(clientId);
    }

    @GetMapping("/payment/{paymentId}")
    public Flux<LedgerEntryReadDto> findByPaymentId(@PathVariable("paymentId") final Long paymentId) {
        return allocationService.findByPaymentId(paymentId);
    }

    @GetMapping("/invoice/{invoiceId}")
    public Flux<LedgerEntryReadDto> findByInvoiceId(@PathVariable("invoiceId") final Long invoiceId) {
        return allocationService.findByInvoiceId(invoiceId);
    }

    @PostMapping("/allocate")
    public Mono<ResponseEntity<Void>> allocate(@RequestBody @Valid final PaymentAllocationCreateUpdateDto dto) {
        return allocationService.allocatePaymentToInvoice(dto.paymentId(), dto.invoiceId(), dto.allocatedAmount())
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/deallocate")
    public Mono<ResponseEntity<Void>> deallocate(@RequestBody @Valid final PaymentAllocationCreateUpdateDto dto) {
        return allocationService.deallocatePaymentFromInvoice(dto.paymentId(), dto.invoiceId(), dto.allocatedAmount())
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
