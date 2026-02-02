package com.sonumax2.javabot.domain.reference;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("nomenclature_usage")
public class NomenclatureUsage {

    @Id @Column("id") private Long id;
    @Column("nomenclature_id") private Long nomenclatureId;
    @Column("usage") private String usage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNomenclatureId() { return nomenclatureId; }
    public void setNomenclatureId(Long nomenclatureId) { this.nomenclatureId = nomenclatureId; }

    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }
}
