package com.sonumax2.javabot.util;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class InputParseUtils {
    private InputParseUtils() {}

    public static BigDecimal parseAmount(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        // убираем пробелы и nbsp
        s = s.replace("\u00A0", "").replace(" ", "");

        // k/к = *1000
        boolean isK = s.toLowerCase().endsWith("k") || s.endsWith("к") || s.endsWith("К");
        if (isK) s = s.substring(0, s.length() - 1);

        // если встречаются и ',' и '.', пытаемся понять где десятичный
        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');
        if (lastComma != -1 && lastDot != -1) {
            if (lastComma > lastDot) {
                // 1.234,56 -> 1234.56
                s = s.replace(".", "").replace(",", ".");
            } else {
                // 1,234.56 -> 1234.56
                s = s.replace(",", "");
            }
        } else {
            // обычный случай: 12,5 -> 12.5
            s = s.replace(",", ".");
        }

        // вычищаем валюты/текст, оставляем цифры, точку, минус
        s = s.replaceAll("[^0-9.\\-]", "");
        if (s.isBlank()) return null;

        // минус только в начале
        s = s.replaceAll("(?<!^)-", "");

        // если много точек — считаем, что последняя десятичная, остальные тысячные
        int last = s.lastIndexOf('.');
        if (last != -1) {
            String intPart = s.substring(0, last).replace(".", "");
            String fracPart = s.substring(last + 1);
            s = intPart + "." + fracPart;
        }

        try {
            BigDecimal v = new BigDecimal(s);
            if (isK) v = v.multiply(BigDecimal.valueOf(1000));
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    public static DateParseResult parseSmartDate(String raw, LocalDate today) {
        if (raw == null) return DateParseResult.err(DateParseResult.Error.INVALID_FORMAT);

        String s = raw.trim().replace("\u00A0", "");
        if (s.isEmpty()) return DateParseResult.err(DateParseResult.Error.INVALID_FORMAT);

        // ISO: yyyy-MM-dd / yyyy.M.d / yyyy/MM/dd
        if (s.matches("^\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}$")) {
            String[] p = s.replace('.', '-').replace('/', '-').split("-");
            try {
                int y = Integer.parseInt(p[0]);
                int m = Integer.parseInt(p[1]);
                int d = Integer.parseInt(p[2]);
                LocalDate dt = safeDate(y, m, d);
                return dt != null ? DateParseResult.ok(dt) : DateParseResult.err(DateParseResult.Error.INVALID_DATE);
            } catch (Exception e) {
                return DateParseResult.err(DateParseResult.Error.INVALID_FORMAT);
            }
        }

        // нормализуем разделители под dd.MM...
        s = s.replace("/", ".").replace("-", ".").replace(" ", "");

        // dd.MM.yyyy
        if (s.matches("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}$")) {
            String[] p = s.split("\\.");
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            LocalDate dt = safeDate(y, m, d);
            return dt != null ? DateParseResult.ok(dt) : DateParseResult.err(DateParseResult.Error.INVALID_DATE);
        }

        // dd.MM.yy
        if (s.matches("^\\d{1,2}\\.\\d{1,2}\\.\\d{2}$")) {
            String[] p = s.split("\\.");
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int yy = Integer.parseInt(p[2]);
            int y = 2000 + yy;
            LocalDate dt = safeDate(y, m, d);
            return dt != null ? DateParseResult.ok(dt) : DateParseResult.err(DateParseResult.Error.INVALID_DATE);
        }

        // dd.MM (год текущий)
        if (s.matches("^\\d{1,2}\\.\\d{1,2}$")) {
            String[] p = s.split("\\.");
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            LocalDate dt = safeDate(today.getYear(), m, d);
            return dt != null ? DateParseResult.ok(dt) : DateParseResult.err(DateParseResult.Error.INVALID_DATE);
        }

        // только день (месяц+год текущие). Если не влезло — просим месяц
        if (s.matches("^\\d{1,2}$")) {
            int d = Integer.parseInt(s);
            LocalDate dt = safeDate(today.getYear(), today.getMonthValue(), d);
            return dt != null ? DateParseResult.ok(dt) : DateParseResult.err(DateParseResult.Error.NEED_MONTH);
        }

        return DateParseResult.err(DateParseResult.Error.INVALID_FORMAT);
    }

    private static LocalDate safeDate(int y, int m, int d) {
        try {
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            return null;
        }
    }
}
