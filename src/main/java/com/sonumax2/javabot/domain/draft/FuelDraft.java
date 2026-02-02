package com.sonumax2.javabot.domain.draft;

import com.sonumax2.javabot.domain.operation.DocType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Черновик создания "Топливо" (FUEL) (живёт в draftService).
 */
public class FuelDraft extends OpDraftBase {

    public FuelKind fuelKind;

    // equipment (транспорт/техника)
    public Long equipmentId;
    public String pendingEquipmentName;

    // counterparty
    public Long counterpartyId;
    public String pendingCounterpartyName;

    // liters / volume
    public BigDecimal volume;

    // common fields
    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно)
    public String docFileId;
    public DocType docType = DocType.NO_RECEIPT;

    public enum FuelKind {
        TRANSPORT, MACHINE
    }
}
