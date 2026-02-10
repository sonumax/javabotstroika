package com.sonumax2.javabot.domain.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("syp_item")
public class SypItem {

    @Id
    @Column("id")
    public Long id;

    @Column("operation_id")
    public Long operationId;

    @Column("nomenclature_id")
    public Long nomenclatureId;

    @Column("volume")
    public BigDecimal volume;
}
