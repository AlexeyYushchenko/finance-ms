package ru.utlc.financialmanagementservice.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("payment_allocation_view")
public class PaymentAllocationView extends AuditingEntity<Long>{
    @Id
    private Long id;
    private BigDecimal totalAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private Boolean isFullyAllocated;
}
