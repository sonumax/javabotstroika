package com.sonumax2.javabot.domain.draft;

import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.reference.CounterpartyKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Черновик создания расхода (живёт в draftService).
 */
public class ExpenseDraft extends OpDraftBase {

    // данные операции
    public Long objectId;

    // nomenclature
    public Long nomenclatureId;
    public String pendingNomenclatureName;

    // counterparty
    public Long counterpartyId;
    public String pendingCounterpartyName;
    public CounterpartyKind counterpartyKind;
    public String counterpartyRawName;

    // common fields
    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно)
    public String docFileId;
    public DocType docType = DocType.NO_RECEIPT;
}
