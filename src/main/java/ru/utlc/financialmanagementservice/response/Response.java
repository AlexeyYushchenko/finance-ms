package ru.utlc.financialmanagementservice.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Response(List<String> errorMessages, Object resource) {
    public Response(String errorMessage) {
        this(List.of(errorMessage), null);
    }

    public Response(Object resource) {
        this(null, resource);
    }
}