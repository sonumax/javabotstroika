package com.sonumax2.javabot.domain.draft;

public enum DraftType {
    EXPENSE("EXPENSE"),
    ADVANCE("ADVANCE"),
    SYP("SYP"),
    FUEL("FUEL");

    private final String key;
    DraftType(String key) { this.key = key; }
    public String key() { return key; }
}