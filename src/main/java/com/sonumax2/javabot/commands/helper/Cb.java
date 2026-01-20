package com.sonumax2.javabot.commands.helper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Cb {
    private static final String SEP = ":";

    private Cb() {}

    /** Собирает callback из частей: cb("exp","obj","pick", 12) -> "exp:obj:pick:12" */
    public static String makeCb(Object... parts) {
        String[] s = Arrays.stream(parts).map(String::valueOf).toArray(String[]::new);
        String res = String.join(SEP, s);

        // Telegram callback_data limit: 64 bytes
        int bytes = res.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > 64) {
            throw new IllegalArgumentException("callback_data too long: " + bytes + " bytes: " + res);
        }
        return res;
    }

    /** Точное совпадение: is(data, "exp","obj","new") */
    public static boolean is(String data, Object... parts) {
        return data != null && data.equals(makeCb(parts));
    }

    /** Проверка префикса: startsWith(data, "exp","obj","pick") */
    public static boolean startsWith(String data, Object... prefixParts) {
        if (data == null) return false;
        String p = makeCb(prefixParts) + SEP;
        return data.startsWith(p);
    }

    /** Вернуть хвост после префикса: tail("exp:obj:pick:12", "exp","obj","pick") -> "12" */
    public static String tail(String data, Object... prefixParts) {
        String p = makeCb(prefixParts) + SEP;
        if (data == null || !data.startsWith(p)) return null;
        return data.substring(p.length());
    }

    public static long tailLong(String data, Object... prefixParts) {
        String t = tail(data, prefixParts);
        if (t == null) throw new IllegalArgumentException("No tail for prefix " + makeCb(prefixParts));
        return Long.parseLong(t);
    }
}
