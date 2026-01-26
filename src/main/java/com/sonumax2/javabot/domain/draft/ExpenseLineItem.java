package com.sonumax2.javabot.domain.draft;

import java.math.BigDecimal;

public class ExpenseLineItem {
    public Long nomenclatureId;
    public BigDecimal volume;
    public BigDecimal price; // опционально

    public ExpenseLineItem() {}

    public ExpenseLineItem(Long nomenclatureId, BigDecimal volume) {
        this.nomenclatureId = nomenclatureId;
        this.volume = volume;
    }
}
