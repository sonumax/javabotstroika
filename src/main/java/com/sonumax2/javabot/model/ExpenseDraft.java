package com.sonumax2.javabot.model;

import com.sonumax2.javabot.model.reference.CounterpartyKind;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseDraft {

    // meta (панель/confirm-режим)
    public Integer panelMessageId;
    public boolean returnToConfirm;

    // данные операции
    public Long objectId;
    public Long nomenclatureId;
    public String pendingNomenclatureName;
    public Long counterpartyId;
    public CounterpartyKind counterpartyKind; // если выбираешь kind при создании
    public String counterpartyRawName;        // если нужно помнить ввод при создании

    public BigDecimal amount;
    public LocalDate date;
    public String note;

    // Telegram file_id (если фото одно). Если хочешь несколько — сделаем List<String>.
    public String photoFileId;

    public void clearData() {
        objectId = null;
        nomenclatureId = null;
        counterpartyId = null;
        counterpartyKind = null;
        counterpartyRawName = null;
        amount = null;
        date = null;
        note = null;
        photoFileId = null;
        returnToConfirm = false;
        pendingNomenclatureName = null;
    }
}
