package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;
import ru.utlc.financialmanagementservice.service.PaymentTypeService;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.util.RequestHandlerUtil;

import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.PAYMENT_TYPES;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(PAYMENT_TYPES)
public class PaymentTypeRestController {

    private final PaymentTypeService paymentTypePayment;
    @GetMapping
    public Mono<ResponseEntity<List<PaymentTypeReadDto>>> findAll() {
        return paymentTypePayment.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentTypeReadDto>> findById(@PathVariable("id") final Integer id) {
        return paymentTypePayment.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json")
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid PaymentTypeCreateUpdateDto dto, BindingResult bindingResult) {
        return RequestHandlerUtil.handleRequest(
                bindingResult,
                paymentTypePayment::create,
                paymentTypeReadDto -> RequestHandlerUtil.createdResponse(paymentTypeReadDto, paymentTypeReadDto.id()),
                dto
        );
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Integer id,
                                                 @RequestBody @Valid PaymentTypeCreateUpdateDto dto,
                                                 BindingResult bindingResult) {
        return RequestHandlerUtil.handleRequest(
                bindingResult,
                d -> paymentTypePayment.update(id, d),
                RequestHandlerUtil::updatedResponse,
                dto
        );
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Integer id) {
        return paymentTypePayment.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}