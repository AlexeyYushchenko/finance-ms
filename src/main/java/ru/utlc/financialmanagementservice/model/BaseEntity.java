package ru.utlc.financialmanagementservice.model;

import java.io.Serializable;
/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface BaseEntity<T extends Serializable>{

    T getId();

    void setId(T id);
}
