package com.sonumax2.javabot.domain.operation;

import com.sonumax2.javabot.domain.draft.FuelKind;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Table("fuel_detail")
public class FuelDetail {

    @Id
    @Column("operation_id") private Long operationId;
    @Column("fuel_kind") private FuelKind fuelKind; // "TRANSPORT" / "MACHINE"
    @Column("machine_type_id") private Long machineTypeId;
    @Column("equipment_id") private Long equipmentId;
    @Column("counterparty_id") private Long counterpartyId;
    @Column("volume") private BigDecimal volume;
    @Column("receipt_type") private DocType docType = DocType.NO_RECEIPT;

    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }

    public FuelKind getFuelKind() { return fuelKind; }
    public void setFuelKind(FuelKind fuelKind) { this.fuelKind = fuelKind; }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    public Long getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(Long counterpartyId) { this.counterpartyId = counterpartyId; }

    public DocType getDocType() { return docType; }
    public void setDocType(DocType docType) { this.docType = docType; }

    public Long getMachineTypeId() { return machineTypeId; }
    public void setMachineTypeId(Long machineTypeId) { this.machineTypeId = machineTypeId; }
}
