package com.sonumax2.javabot.commands.helper;

public final class AdvanceCb {
    private AdvanceCb() {}

    public static final String NS = "adv";

    // prefixes
    public static String amountPrefix() { return Cb.makeCb(NS, CbParts.AMOUNT); }
    public static String notePrefix() { return Cb.makeCb(NS, CbParts.NOTE); }
    public static String datePrefix()   { return Cb.makeCb(NS, CbParts.DATE); }
    public static String confirmPrefix()    { return Cb.makeCb(NS, CbParts.CONFIRM); }

    // exact
    public static String start()      { return Cb.makeCb(CbParts.ADD_OPR, NS); }
    public static String noteSkip()   { return Cb.makeCb(notePrefix(), CbParts.SKIP); }
    public static String noteBack()   { return Cb.makeCb(notePrefix(), CbParts.BACK); }

    public static String amountBack()   { return Cb.makeCb(amountPrefix(), CbParts.BACK); }
    public static String errorAmountBack() { return Cb.makeCb(amountPrefix(), CbParts.ERR, CbParts.BACK); }

    public static String errorDateBack() { return Cb.makeCb(datePrefix(), CbParts.ERR, CbParts.BACK); }
    public static String manualDateBack() { return Cb.makeCb(datePrefix(), CbParts.MANUAL, CbParts.BACK);}
    public static String confirmBack() { return confirm(CbParts.BACK);}
    public static String confirmCancel() { return confirm(CbParts.CANCEL);}

    // builders
    public static String datePick(String isoDate) { return Cb.makeCb(datePrefix(), isoDate); }
    public static String confirm(String action)  { return Cb.makeCb(confirmPrefix(), action); }

    // parsers
    public static String dateIso(String data)     { return Cb.tail(data, datePrefix()); } // вернёт yyyy-mm-dd
    public static String getConfirmAction(String data)  { return Cb.tail(data, confirmPrefix()); }

    public static boolean isDatePick(String data) { return data != null && data.startsWith(datePrefix()); }
    public static boolean isDateManualPick(String data) {return Cb.is(data, datePrefix(), CbParts.MANUAL);}
    public static boolean isDateYesterdayPick(String data) {return Cb.is(data, datePrefix(), CbParts.YESTERDAY);}
    public static boolean isDateErrorBackPick(String data) {return Cb.is(data, errorDateBack()); }
    public static boolean isDateManualBackPick(String data) {return Cb.is(data, manualDateBack()); }

    public static boolean isAmountBackPick(String data) {return Cb.is(data, amountBack()); }
    public static boolean isAmountErrorBackPick(String data) {return Cb.is(data, errorAmountBack()); }

    public static boolean isStartPick(String data) { return Cb.is(data, start()); }
    public static boolean isNoteSkipPick(String data) { return Cb.is(data, noteSkip()); }
    public static boolean isNoteBackPick(String data) { return Cb.is(data, noteBack()); }

    public static boolean isConfirmPick(String data) { return data != null && data.startsWith(confirmPrefix()); }
    public static boolean isConfirmSavePick (String data) { return Cb.is(data, confirm(CbParts.SAVE)); }
    public static boolean isConfirmEditDatePick (String data) { return Cb.is(data, confirm(CbParts.EDIT_DATE)); }
    public static boolean isConfirmEditAmountPick (String data) { return Cb.is(data, confirm(CbParts.EDIT_AMOUNT)); }
    public static boolean isConfirmEditNotePick (String data) { return Cb.is(data, confirm(CbParts.EDIT_NOTE)); }
    public static boolean isConfirmCancelPick (String data) { return Cb.is(data, confirmCancel()); }
    public static boolean isConfirmBackPick (String data) { return Cb.is(data, confirmBack()); }

}