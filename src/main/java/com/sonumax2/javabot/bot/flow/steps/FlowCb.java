package com.sonumax2.javabot.bot.flow.steps;

import com.sonumax2.javabot.bot.commands.cb.Cb;

public final class FlowCb {
    private FlowCb() {}

    public static String cb(String ns, String step, String action, Object... tail) {
        if (tail == null || tail.length == 0) {
            return Cb.makeCb(ns, step, action);
        }
        Object[] parts = new Object[3 + tail.length];
        parts[0] = ns;
        parts[1] = step;
        parts[2] = action;
        System.arraycopy(tail, 0, parts, 3, tail.length);
        return Cb.makeCb(parts);
    }

    public static boolean is(String data, String ns, String step, String action) {
        return Cb.is(data, ns, step, action);
    }

    public static boolean startsWith(String data, String ns, String step, String action) {
        return Cb.startsWith(data, ns, step, action);
    }

    public static String tail(String data, String ns, String step, String action) {
        return Cb.tail(data, ns, step, action);
    }

    public static long tailLong(String data, String ns, String step, String action) {
        return Cb.tailLong(data, ns, step, action);
    }
}
