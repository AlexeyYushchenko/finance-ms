package ru.utlc.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Table("rate_update_log")
public class RateUpdateLog extends AuditingEntity<Integer> {

    @Id
    private Integer id;
    private LocalDate updateDate;
    private Boolean status;
}
