package ru.utlc.financialmanagementservice.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Response(List<String> errorMessages, Object resource) {
    public Response(String errorMessage) {
        this(List.of(errorMessage), null);
    }
    public Response(Object resource) {
        this(null, resource);
    }
}