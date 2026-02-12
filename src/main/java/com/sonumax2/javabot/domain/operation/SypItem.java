package com.sonumax2.javabot.domain.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("syp_item")
public class SypItem {

    @Id private Long id;
    @Column("nomenclature_id") private Long nomenclatureId;
    @Column("volume") private BigDecimal volume;

    public Long getId() { return id; }

    public Long getNomenclatureId() { return nomenclatureId; }
    public void setNomenclatureId(Long nomenclatureId) { this.nomenclatureId = nomenclatureId; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
}
