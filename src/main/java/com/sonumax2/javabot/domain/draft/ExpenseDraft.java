package com.sonumax2.javabot.domain.draft;

import com.sonumax2.javabot.domain.operation.ReceiptType;
import com.sonumax2.javabot.domain.reference.CounterpartyKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Черновик создания расхода (живёт в draftService).
 */
public class ExpenseDraft {

    // meta (панель/confirm-режим)
    public Integer panelMessageId;
    public boolean returnToConfirm;

    // данные операции
    public Long objectId;

    // nomenclature
    public Long nomenclatureId;
    public String pendingNomenclatureName; // когда создаём новую, но ещё не сохранили справочник

    // counterparty
    public Long counterpartyId;
    public CounterpartyKind counterpartyKind; // если при создании выбираешь kind
    public String counterpartyRawName;        // ввод имени при создании

    // common fields
    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно)
    public String photoFileId;
    public ReceiptType receiptType = ReceiptType.RECEIPT;
}
