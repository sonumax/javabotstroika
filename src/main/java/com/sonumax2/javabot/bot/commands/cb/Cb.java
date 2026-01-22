package com.sonumax2.javabot.bot.commands.cb;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Cb {
    private static final String SEP = ":";
    private static final int MAX_CALLBACK_BYTES = 64;

    private Cb() {}

    /**
     * Собирает callback из частей:
     * makeCb("exp","obj","pick", 12) -> "exp:obj:pick:12"
     *
     * Telegram callback_data limit: 64 bytes (UTF-8)
     */
    public static String makeCb(Object... parts) {
        String res = join(parts);
        enforceLimit(res);
        return res;
    }

    /** Точное совпадение: is(data, "exp","obj","new") */
    public static boolean is(String data, Object... parts) {
        if (data == null) return false;
        return data.equals(makeCb(parts));
    }

    /**
     * Проверка префикса с обязательным разделителем:
     * startsWith("exp:obj:pick:12", "exp","obj","pick") -> true
     * startsWith("exp:obj:pick",    "exp","obj","pick") -> false (нет ":" после префикса)
     *
     * Это полезно именно для шаблонов вида "...:<tail>" (например pick:<id>).
     */
    public static boolean startsWith(String data, Object... prefixParts) {
        if (data == null) return false;
        String p = makeCb(prefixParts) + SEP; // важно: с ":" в конце
        return data.startsWith(p);
    }

    /** Удобно иногда руками: prefix("exp","obj","pick") -> "exp:obj:pick:" */
    public static String prefix(Object... prefixParts) {
        return makeCb(prefixParts) + SEP;
    }

    /**
     * Вернуть хвост после префикса:
     * tail("exp:obj:pick:12", "exp","obj","pick") -> "12"
     */
    public static String tail(String data, Object... prefixParts) {
        if (data == null) return null;
        String p = makeCb(prefixParts) + SEP;
        if (!data.startsWith(p)) return null;
        return data.substring(p.length());
    }

    public static long tailLong(String data, Object... prefixParts) {
        String p = makeCb(prefixParts);
        String t = tail(data, prefixParts);

        if (t == null) {
            throw new IllegalArgumentException(
                    "No tail for prefix '" + p + ":' in data: " + data
            );
        }

        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Tail is not a long for prefix '" + p + ":'; tail='" + t + "', data=" + data,
                    e
            );
        }
    }

    // ---------------- internal ----------------

    private static String join(Object... parts) {
        String[] s = Arrays.stream(parts).map(String::valueOf).toArray(String[]::new);
        return String.join(SEP, s);
    }

    private static void enforceLimit(String cb) {
        int bytes = cb.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_CALLBACK_BYTES) {
            throw new IllegalArgumentException(
                    "callback_data too long: " + bytes + " bytes (max " + MAX_CALLBACK_BYTES + "): " + cb
            );
        }
    }
}
