package ru.utlc.financialmanagementservice.constants;

import lombok.experimental.UtilityClass;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@UtilityClass
public class RefTypeConstants {
    // Reference Type IDs
    public static final int REFTYPE_PAYMENT            = 1;
    public static final int REFTYPE_INVOICE            = 2;
    public static final int REFTYPE_ALLOCATION         = 3;
    public static final int REFTYPE_CONVERSION         = 4;
    public static final int REFTYPE_PAYMENT_ADJUSTMENT = 5;
    public static final int REFTYPE_PAYMENT_REVERSAL   = 6;
    public static final int REFTYPE_INVOICE_ADJUSTMENT = 7;
    public static final int REFTYPE_INVOICE_REVERSAL   = 8;

    public static final int RUB_CURRENCY_ID = 1;
    public static final int USD_CURRENCY_ID = 2;
    public static final int EUR_CURRENCY_ID = 3;
    public static final int CNY_CURRENCY_ID = 4;

}
