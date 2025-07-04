package ru.utlc.finance.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.utlc.finance.api.dto.Auditable;
/*
 * Copyright (c) 2024, Aleksey Yushchenko (13yae13@gmail.com). All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Aleksey Yushchenko (13yae13@gmail.com)
 * Date: 2024-08-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrencyDto(
        Integer id,
        String code,
        String okvCode,
        String name,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) implements Auditable {}
