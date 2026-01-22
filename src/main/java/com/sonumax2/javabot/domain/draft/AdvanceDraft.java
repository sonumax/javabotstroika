package com.sonumax2.javabot.domain.draft;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdvanceDraft {
    public Integer panelMessageId;
    public LocalDate date;
    public BigDecimal amount;
    public String note;
    public boolean returnToConfirm;

}