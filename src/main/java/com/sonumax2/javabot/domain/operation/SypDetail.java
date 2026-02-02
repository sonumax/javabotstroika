package com.sonumax2.javabot.domain.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("syp_detail")
public class SypDetail {

    @Id
    public Long operationId;

    public Long workObjectId;
    public Long counterpartyId;

    public String payType; // "CASH" / "CASHLESS"
}
