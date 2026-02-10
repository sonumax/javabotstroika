package com.sonumax2.javabot.domain.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("syp_detail")
public class SypDetail {

    @Id @Column("operation_id") private Long operationId;

    @Column("work_object_id") private Long workObjectId;
    @Column("counterparty_id") private Long counterpartyId;

    @Column("pay_type") private PayType payType; // "CASH" / "CASHLESS"
    @Column("receipt_type") private DocType docType = DocType.NO_RECEIPT;

    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }

    public Long getWorkObjectId() { return workObjectId; }
    public void setWorkObjectId(Long workObjectId) { this.workObjectId = workObjectId; }

    public Long getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(Long counterpartyId) { this.counterpartyId = counterpartyId; }

    public PayType getPayType() { return payType; }
    public void setPayType(PayType payType) { this.payType = payType; }

    public DocType getDocType() { return docType; }
    public void setDocType(DocType docType) { this.docType = docType; }
}
