package com.sonumax2.javabot.domain.operation;

import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Table("fuel_detail")
public class FuelDetail {

    @Id
    public Long operationId;

    public String fuelKind; // "TRANSPORT" / "MACHINE"
    public Long equipmentId;
    public Long counterpartyId;

    public BigDecimal volume;
}
