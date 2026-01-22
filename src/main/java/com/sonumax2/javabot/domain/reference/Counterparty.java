package com.sonumax2.javabot.domain.reference;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("counterparty")
public class Counterparty extends BaseRefEntity {
    @Column("kind") private CounterpartyKind kind = CounterpartyKind.OTHER;

    public CounterpartyKind getKind() {return kind;}
    public void setKind(CounterpartyKind kind) {this.kind = kind;}
}
