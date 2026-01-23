package com.sonumax2.javabot.domain.operation;

public enum DocType {
    RECEIPT,      // есть чек
    INVOICE,      // накладная
    NO_RECEIPT;    // без чека

    public boolean needsFile() {
        return this == RECEIPT || this == INVOICE;
    }
}
