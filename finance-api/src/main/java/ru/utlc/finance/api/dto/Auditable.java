// finance-api/src/main/java/ru/utlc/finance/api/dto/Auditable.java
package ru.utlc.finance.api.dto;

import java.time.LocalDateTime;
/*
 * Copyright (c) 2024, Aleksey Yushchenko (13yae13@gmail.com). All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Aleksey Yushchenko (13yae13@gmail.com)
 * Date: 2024-08-19
 */
/** Common audit fields exposed to outside consumers. */
public interface Auditable {
    LocalDateTime createdAt();
    LocalDateTime modifiedAt();
}
