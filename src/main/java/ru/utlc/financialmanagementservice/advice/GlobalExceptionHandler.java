package ru.utlc.financialmanagementservice.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.exception.*;
import ru.utlc.financialmanagementservice.response.Response;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */


@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Response>> handleValidationExceptions(WebExchangeBindException ex) {
        log.error(ex.getMessage());
        List<String> errorMessages = getLocalizedErrorMessages(ex.getFieldErrors());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Response(errorMessages, null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error(ex.getMessage());
        List<String> errorMessages = getLocalizedErrorMessages(ex.getFieldErrors());
        Response response = new Response(errorMessages, null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleBindException(BindException ex) {
        log.error(ex.getMessage());
        List<String> errorMessages = getLocalizedErrorMessages(ex.getFieldErrors());
        Response response = new Response(errorMessages, null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(CONFLICT)
    public ResponseEntity<Response> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String errorMessage;
        log.error(ex.getMessage());
        if (isForeignKeyConstraintViolation(ex)) {
            errorMessage = messageSource.getMessage("error.database.foreignKeyConstraintViolation", null, LocaleContextHolder.getLocale());
        } else if (isUniqueConstraintViolation(ex)) {
            errorMessage = messageSource.getMessage("error.database.uniqueConstraintViolation", null, LocaleContextHolder.getLocale());
        } else {
            errorMessage = messageSource.getMessage("error.database.genericIntegrityViolation", null, LocaleContextHolder.getLocale());
        }

        return ResponseEntity.status(CONFLICT).body(new Response(errorMessage));
    }

    private boolean isForeignKeyConstraintViolation(DataIntegrityViolationException ex) {
        log.error(ex.getMessage());
        return ex.getMessage() != null && ex.getMessage().contains("violates foreign key constraint");
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        log.error(ex.getMessage());
        return ex.getMessage() != null && ex.getMessage().contains("violates unique constraint");
    }

    @ExceptionHandler(CurrencyCreationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleEntityCreationException(CurrencyCreationException ex) {
        log.error(ex.getMessage());
        String errorMessage = messageSource.getMessage("error.currency.creation", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(PaymentUpdateException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleEntityUpdateException(PaymentUpdateException ex) {
        log.error(ex.getMessage());
        String errorMessage = messageSource.getMessage("error.payment.update", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(InvoiceUpdateException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleEntityUpdateException(InvoiceUpdateException ex) {
        log.error(ex.getMessage());
        String errorMessage = messageSource.getMessage("error.invoice.update", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(ServiceTypeCreationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleEntityCreationException(ServiceTypeCreationException ex) {
        log.error(ex.getMessage());
        String errorMessage = messageSource.getMessage("error.serviceType.creation", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleValidationException(ValidationException ex) {
        log.error("Validation error: {}", ex.getMessageKey());
        String errorMessage = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(AbstractNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Response> handleAllNotFoundExceptions(AbstractNotFoundException ex) {
        String errorMessage = messageSource.getMessage(ex.getMessage(), ex.getArgs(), LocaleContextHolder.getLocale());
        log.error(errorMessage);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(errorMessage));
    }

    @ExceptionHandler(ExchangeRateRetrievalFailedException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ResponseEntity<Response> handleExchangeRateNotFoundException(ExchangeRateRetrievalFailedException ex) {
        String errorMessage = messageSource.getMessage(ex.getMessage(), ex.getArgs(), LocaleContextHolder.getLocale());
        log.error(errorMessage);
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ResponseEntity<Response> handleGeneralException(Exception ex) {
        log.error(ex.getMessage());
        String errorMessage = messageSource.getMessage("error.general", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new Response(errorMessage));
    }

    private List<String> getLocalizedErrorMessages(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(fieldError -> messageSource.getMessage(fieldError, LocaleContextHolder.getLocale()))
                .toList();
    }
}
