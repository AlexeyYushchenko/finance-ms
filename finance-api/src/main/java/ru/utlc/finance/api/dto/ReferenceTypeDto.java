package ru.utlc.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
/*
 * Copyright (c) 2024, Aleksey Yushchenko (13yae13@gmail.com). All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Aleksey Yushchenko (13yae13@gmail.com)
 * Date: 2024-08-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReferenceTypeDto(
        Integer id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) implements Auditable {}
