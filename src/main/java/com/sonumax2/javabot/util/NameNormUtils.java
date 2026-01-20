package com.sonumax2.javabot.util;

import java.util.Locale;

public final class NameNormUtils {
    private NameNormUtils() {}

    public static String normalizeUi(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeNorm(String raw) {
        if (raw == null) return "";
        return normalizeUi(raw).toLowerCase(Locale.ROOT);
    }
}