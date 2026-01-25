//package com.sonumax2.javabot.bot.commands.cb;
//
//public final class AdvanceCb {
//    private AdvanceCb() {}
//
//    public static final String NS = "adv";
//
//    // ---------------- helpers (только внутри класса) ----------------
//
//    private static String ns(String part) {
//        return Cb.makeCb(NS, part);
//    }
//
//    private static String act(String prefix, String action) {
//        return Cb.makeCb(prefix, action);
//    }
//
//    private static String back(String prefix) {
//        return Cb.makeCb(prefix, CbParts.BACK);
//    }
//
//    private static boolean eq(String data, String expected) {
//        return Cb.is(data, expected);
//    }
//
//    private static boolean starts(String data, String prefix) {
//        return Cb.startsWith(data, prefix);
//    }
//
//    // ---------------- prefixes ----------------
//
//    public static String amountPrefix()  { return ns(CbParts.AMOUNT); }
//    public static String notePrefix()    { return ns(CbParts.NOTE); }
//    public static String datePrefix()    { return ns(CbParts.DATE); }
//    public static String confirmPrefix() { return ns(CbParts.CONFIRM); }
//
//    // ---------------- start ----------------
//
//    public static String start() { return Cb.makeCb(CbParts.ADD_OPR, NS); }
//    public static boolean isStartPick(String data) { return eq(data, start()); }
//
//    // ---------------- note ----------------
//
//    public static String noteSkip() { return act(notePrefix(), CbParts.SKIP); }
//    public static String noteBack() { return back(notePrefix()); }
//
//    public static boolean isNoteSkipPick(String data) { return eq(data, noteSkip()); }
//    public static boolean isNoteBackPick(String data) { return eq(data, noteBack()); }
//
//    // ---------------- amount ----------------
//
//    public static String amountBack() { return back(amountPrefix()); }
//    public static String errorAmountBack() { return Cb.makeCb(amountPrefix(), CbParts.ERR, CbParts.BACK); }
//
//    public static boolean isAmountBackPick(String data) { return eq(data, amountBack()); }
//    public static boolean isAmountErrorBackPick(String data) { return eq(data, errorAmountBack()); }
//
//    // ---------------- date ----------------
//
//    /** adv:date */
//    public static boolean isDate(String data) { return starts(data, datePrefix()); }
//    public static boolean isDateManualPick(String data) { return data != null && Cb.is(data, datePrefix(), CbParts.MANUAL); }
//    public static boolean isDateYesterdayPick(String data) { return data != null && Cb.is(data, datePrefix(), CbParts.YESTERDAY); }
//
//    public static String errorDateBack() { return Cb.makeCb(datePrefix(), CbParts.ERR, CbParts.BACK); }
//    public static String manualDateBack() { return Cb.makeCb(datePrefix(), CbParts.MANUAL, CbParts.BACK); }
//
//    public static boolean isDateErrorBackPick(String data) { return eq(data, errorDateBack()); }
//    public static boolean isDateManualBackPick(String data) { return eq(data, manualDateBack()); }
//
//    /** adv:date:yyyy-mm-dd */
//    public static String datePick(String isoDate) { return Cb.makeCb(datePrefix(), isoDate); }
//    /** Вернёт yyyy-mm-dd */
//    public static String dateIso(String data) { return Cb.tail(data, datePrefix()); }
//
//    // ---------------- confirm ----------------
//
//    public static String confirm(String action) { return act(confirmPrefix(), action); }
//
//    public static String confirmBack() { return confirm(CbParts.BACK); }
//    public static String confirmCancel() { return confirm(CbParts.CANCEL); }
//
//    public static boolean isConfirmPick(String data) { return data != null && data.startsWith(confirmPrefix()); }
//    public static String getConfirmAction(String data) { return Cb.tail(data, confirmPrefix()); }
//
//    public static boolean isConfirmSavePick(String data) { return eq(data, confirm(CbParts.SAVE)); }
//    public static boolean isConfirmEditDatePick(String data) { return eq(data, confirm(CbParts.EDIT_DATE)); }
//    public static boolean isConfirmEditAmountPick(String data) { return eq(data, confirm(CbParts.EDIT_AMOUNT)); }
//    public static boolean isConfirmEditNotePick(String data) { return eq(data, confirm(CbParts.EDIT_NOTE)); }
//    public static boolean isConfirmCancelPick(String data) { return eq(data, confirmCancel()); }
//    public static boolean isConfirmBackPick(String data) { return eq(data, confirmBack()); }
//}
