package com.sonumax2.javabot.domain.draft;

import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.reference.CounterpartyKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Черновик создания расхода (живёт в draftService).
 */
public class ExpenseDraft extends OpDraftBase {

    public String pendingObjectName;
    public Long objectId;

    // nomenclature
    public Long nomenclatureId;
    public String pendingNomenclatureName;

    // counterparty
    public Long counterpartyId;
    public String pendingCounterpartyName;
    public CounterpartyKind counterpartyKind;

    // common fields
    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно)
    public String docFileId;
    public DocType docType = DocType.NO_RECEIPT;
}
