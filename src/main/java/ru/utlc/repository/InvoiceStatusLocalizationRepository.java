package ru.utlc.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.localization.InvoiceStatusLocalization;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface InvoiceStatusLocalizationRepository extends R2dbcRepository<InvoiceStatusLocalization, Integer> {
}
