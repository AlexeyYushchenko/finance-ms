package ru.utlc.financialmanagementservice.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.utlc.financialmanagementservice.exception.CurrencyCreationException;
import ru.utlc.financialmanagementservice.exception.ServiceTypeCreationException;
import ru.utlc.financialmanagementservice.response.Response;

import java.util.Arrays;
import java.util.List;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        Response response = new Response(errorMessages, null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleBindException(BindException ex) {
        List<String> errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        Response response = new Response(errorMessages, null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(CONFLICT)
    public ResponseEntity<Response> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String errorMessage;

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
        return ex.getMessage() != null && ex.getMessage().contains("violates foreign key constraint");
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("violates unique constraint");
    }

    @ExceptionHandler(CurrencyCreationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleEntityCreationException(CurrencyCreationException ex) {
        String errorMessage = messageSource.getMessage("error.entity.currency.creation", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(ServiceTypeCreationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ResponseEntity<Response> handleEntityCreationException(ServiceTypeCreationException ex) {
        String errorMessage = messageSource.getMessage("error.entity.serviceType.creation", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(BAD_REQUEST).body(new Response(errorMessage));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ResponseEntity<Response> handleGeneralException(Exception ex) {
        System.out.println(ex.getMessage());
        System.out.println(Arrays.toString(ex.getStackTrace()));

        String errorMessage = messageSource.getMessage("error.general", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new Response(errorMessage));
    }
}
