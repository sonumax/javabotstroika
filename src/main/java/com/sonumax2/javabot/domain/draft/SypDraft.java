package com.sonumax2.javabot.domain.draft;

import com.sonumax2.javabot.domain.operation.DocType;

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

    // pay type (если нужно; если нет — можешь удалить)
    public String payType;

    // items (материал + объем)
    public List<SypItem> items = new ArrayList<>();

    // common fields
    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно)
    public String docFileId;
    public DocType docType = DocType.NO_RECEIPT;

    public static class SypItem {
        public Long nomenclatureId;
        public String pendingNomenclatureName;
        public BigDecimal volume;
    }
}
