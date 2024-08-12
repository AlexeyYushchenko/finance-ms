package ru.utlc.financialmanagementservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.financialmanagementservice.response.Response;
import ru.utlc.financialmanagementservice.service.ServiceTypeService;
import ru.utlc.financialmanagementservice.util.RequestHandlerUtil;

import java.util.List;

import static ru.utlc.financialmanagementservice.constants.ApiPaths.SERVICE_TYPES;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SERVICE_TYPES)
public class ServiceTypeRestController {

    private final ServiceTypeService serviceTypeService;
    @GetMapping
    public Mono<ResponseEntity<List<ServiceTypeReadDto>>> findAll() {
        return serviceTypeService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ServiceTypeReadDto>> findById(@PathVariable("id") final Integer id) {
        return serviceTypeService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json")
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid ServiceTypeCreateUpdateDto dto, BindingResult bindingResult) {
        return RequestHandlerUtil.handleRequest(
                bindingResult,
                serviceTypeService::create,
                serviceTypeReadDto -> RequestHandlerUtil.createdResponse(serviceTypeReadDto, serviceTypeReadDto.id()),
                dto
        );
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Integer id,
                                                 @RequestBody @Valid ServiceTypeCreateUpdateDto dto,
                                                 BindingResult bindingResult) {
        return RequestHandlerUtil.handleRequest(
                bindingResult,
                d -> serviceTypeService.update(id, d),
                RequestHandlerUtil::updatedResponse,
                dto
        );
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Integer id) {
        return serviceTypeService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}