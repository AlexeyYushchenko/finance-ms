package ru.utlc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.utlc.dto.servicetype.ServiceTypeCreateUpdateDto;
import ru.utlc.dto.servicetype.ServiceTypeReadDto;
import ru.utlc.response.Response;
import ru.utlc.service.ServiceTypeService;

import java.net.URI;
import java.util.List;

import static ru.utlc.constants.ApiPaths.SERVICE_TYPES;

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
    public Mono<ResponseEntity<Response>> create(@RequestBody @Valid ServiceTypeCreateUpdateDto dto) {
        return serviceTypeService.create(dto)
                .map(serviceTypeReadDto -> {
                    URI location = URI.create("/serviceTypes/" + serviceTypeReadDto.id());
                    return ResponseEntity.created(location).body(new Response(serviceTypeReadDto));
                });
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public Mono<ResponseEntity<Response>> update(@PathVariable("id") final Integer id,
                                                 @RequestBody @Valid ServiceTypeCreateUpdateDto dto) {
        return serviceTypeService.update(id, dto)
                .map(updatedDto -> ResponseEntity.ok(new Response(updatedDto)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") final Integer id) {
        return serviceTypeService.delete(id)
                .flatMap(deleted -> Boolean.TRUE.equals(deleted)
                        ? Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))
                        : Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
    }
}