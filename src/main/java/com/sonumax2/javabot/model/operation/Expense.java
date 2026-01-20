package com.sonumax2.javabot.model.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("expense")
public class Expense {

    @Id @Column("operation_id") private Long operationId;
    @Column("object_id") private Long objectId;
    @Column("nomenclature_id") private Long nomenclatureId;
    @Column("counterparty_id") private Long counterpartyId; // nullable

    public Expense() {}

    public Expense(Long operationId, Long objectId, Long nomenclatureId, Long counterpartyId) {
        this.operationId = operationId;
        this.objectId = objectId;
        this.nomenclatureId = nomenclatureId;
        this.counterpartyId = counterpartyId;
    }

    public Long getOperationId() {return operationId;}
    public void setOperationId(Long operationId) {this.operationId = operationId;}

    public Long getObjectId() {return objectId;}
    public void setObjectId(Long objectId) {this.objectId = objectId;}

    public Long getNomenclatureId() {return nomenclatureId;}
    public void setNomenclatureId(Long nomenclatureId) {this.nomenclatureId = nomenclatureId;}

    public Long getCounterpartyId() {return counterpartyId;}
    public void setCounterpartyId(Long counterpartyId) {this.counterpartyId = counterpartyId;}
}
