package ru.utlc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */

@EqualsAndHashCode(callSuper=true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("client_balance")
public class ClientBalance extends AuditingEntity<Long> {
    
    @Id
    private Long id;
    
    private Integer clientId;
    
    private Integer currencyId;
    
    private BigDecimal balance;

    @Version
    private Long version;
    
    public ClientBalance(Integer clientId, Integer currencyId, BigDecimal balance) {
        this.clientId = clientId;
        this.currencyId = currencyId;
        this.balance = balance;
    }
}
