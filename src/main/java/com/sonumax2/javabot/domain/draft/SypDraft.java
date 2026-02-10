package com.sonumax2.javabot.domain.draft;

import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.PayType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Черновик создания "Сыпучка" (SYP) (живёт в draftService).
 */
public class SypDraft extends OpDraftBase {

    // work object
    public String pendingObjectName;
    public Long objectId;

    // counterparty
    public Long counterpartyId;
    public String pendingCounterpartyName;

    public PayType payType;

    // items (материал + объем)
    public List<ExpenseLineItem> items = new ArrayList<>();
    public Long pendingNomenclatureId;
    public String pendingNomenclatureName;


    // common fields
    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно)
    public String docFileId;
    public DocType docType = DocType.NO_RECEIPT;
}
