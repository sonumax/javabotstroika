package com.sonumax2.javabot.bot.commands.cb;

public class ExpenseCb {
    private ExpenseCb() {}

    public static final String NS = "exp";

    private static String ns(String part) {
        return Cb.makeCb(NS, part);
    }

    private static String act(String prefix, String action) {
        return Cb.makeCb(prefix, action);
    }

    private static String actNs(String part, String action) {
        return Cb.makeCb(NS, part, action);
    }

    private static String back(String prefix) {
        return Cb.makeCb(prefix, CbParts.BACK);
    }

    private static String pick(String prefix) {
        return act(prefix, CbParts.PICK);
    }

    private static boolean eq(String data, String expected) {
        return data != null && Cb.is(data, expected);
    }

    private static boolean starts(String data, String prefix) {
        return data != null && Cb.startsWith(data, prefix);
    }

    // ---------------- start ----------------

    public static String start() { return Cb.makeCb(CbParts.ADD_OPR, NS); }
    public static boolean isStartPick(String data) { return eq(data, start()); }

    // ---------------- object ----------------

    public static String objectPrefix() { return ns(CbParts.OBJ); }
    public static String object(String action) { return act(objectPrefix(), action); }

    public static String newObject() { return object(CbParts.NEW); }
    public static String pickObject() { return object(CbParts.PICK); }
    public static String newObjectBack() { return back(newObject()); }

    public static String getObjectAction(String data) { return Cb.tail(data, objectPrefix()); }
    public static long getNewObjectId(String data) { return Cb.tailLong(data, newObject()); }
    public static long pickObjectId(String data) { return Cb.tailLong(data, pickObject()); }

    public static boolean isObject(String data) { return starts(data, objectPrefix()); }
    public static boolean isObjectNewPick(String data) { return eq(data, newObject()); }
    /** exp:obj:pick:<id> */
    public static boolean isObjectPick(String data) { return starts(data, pickObject()); }
    public static boolean isNewObjectBackPick(String data) { return eq(data, newObjectBack()); }

    // ---------------- nomenclature ----------------

    public static String nomenclaturePrefix() { return ns(CbParts.NOMENCLATURE); }
    public static String nomenclature(String action) { return act(nomenclaturePrefix(), action); }

    public static String newNomenclature() { return nomenclature(CbParts.NEW); }
    public static String createNomenclature() { return nomenclature(CbParts.CREATE); }
    public static String pickNomenclature() { return nomenclature(CbParts.PICK); }
    public static String nomenclatureBack() { return nomenclature(CbParts.BACK); }

    public static String newNomenclatureBack() { return back(newNomenclature()); }
    public static String searchBackNomenclature() { return Cb.makeCb(nomenclaturePrefix(), CbParts.SEARCH, CbParts.BACK); }

    public static long pickNomenclatureId(String data) { return Cb.tailLong(data, pickNomenclature()); }
    public static String nomenclatureAction(String data) { return Cb.tail(data, nomenclaturePrefix()); }

    public static boolean isNomenclature(String data) { return starts(data, nomenclaturePrefix()); }
    public static boolean isNomenclatureNewPick(String data) { return eq(data, newNomenclature()); }
    /** exp:nomenclature:pick:<id> */
    public static boolean isNomenclaturePick(String data) { return starts(data, pickNomenclature()); }
    public static boolean isNomenclatureBackPick(String data) { return eq(data, nomenclatureBack()); }
    public static boolean isNewNomenclatureBackPick(String data) { return eq(data, newNomenclatureBack()); }
    public static boolean isCreateNomenclaturePick(String data) { return eq(data, createNomenclature()); }
    public static boolean isSearchBackNomenclaturePick(String data) { return eq(data, searchBackNomenclature()); }

    // ---------------- counterparty ----------------

    public static String counterpartyPrefix() { return ns(CbParts.COUNTERPARTY); }
    public static String counterparty(String action) { return act(counterpartyPrefix(), action); }

    public static String createCounterparty() { return counterparty(CbParts.CREATE); }
    public static String newCounterparty() { return counterparty(CbParts.NEW); }
    public static String pickCounterparty() { return counterparty(CbParts.PICK); }
    public static String counterpartyBack() { return counterparty(CbParts.BACK); }

    public static String createCounterpartyBack() { return back(createCounterparty()); }
    public static String newCounterpartyBack() { return back(newCounterparty()); }

    public static String counterpartyAction(String data) { return Cb.tail(data, counterpartyPrefix()); }
    public static long pickCounterpartyId(String data) { return Cb.tailLong(data, pickCounterparty()); }

    public static boolean isCounterparty(String data) { return starts(data, counterpartyPrefix()); }
    public static boolean isCounterpartyCreatePick(String data) { return eq(data, createCounterparty()); }
    public static boolean isCounterpartyNewPick(String data) { return eq(data, newCounterparty()); }
    /** exp:counterparty:pick:<id> */
    public static boolean isCounterpartyPick(String data) { return starts(data, pickCounterparty()); }
    public static boolean isNewCounterpartyBackPick(String data) { return eq(data, newCounterpartyBack()); }
    public static boolean isCounterpartyBackPick(String data) { return eq(data, counterpartyBack()); }
    public static boolean isCreateCounterpartyBackPick(String data) { return eq(data, createCounterpartyBack()); }

    // ---------------- amount ----------------

    public static String amountPrefix() { return ns(CbParts.AMOUNT); }
    public static String amountBack() { return back(amountPrefix()); }
    public static String errorAmountBack() { return Cb.makeCb(amountPrefix(), CbParts.ERR, CbParts.BACK); }

    public static boolean isAmountBackPick(String data) { return eq(data, amountBack()); }
    public static boolean isAmountErrorBackPick(String data) { return eq(data, errorAmountBack()); }

    // ---------------- note ----------------

    public static String notePrefix() { return ns(CbParts.NOTE); }
    public static String noteSkip() { return act(notePrefix(), CbParts.SKIP); }
    public static String noteBack() { return back(notePrefix()); }

    public static boolean isNoteSkipPick(String data) { return eq(data, noteSkip()); }
    public static boolean isNoteBackPick(String data) { return eq(data, noteBack()); }

    // ---------------- date ----------------

    public static String datePrefix() { return ns(CbParts.DATE); }

    public static String errorDateBack() { return Cb.makeCb(datePrefix(), CbParts.ERR, CbParts.BACK); }
    public static String manualDateBack() { return Cb.makeCb(datePrefix(), CbParts.MANUAL, CbParts.BACK); }

    public static boolean isDate(String data) { return starts(data, datePrefix()); }
    public static boolean isDateManualPick(String data) { return data != null && Cb.is(data, datePrefix(), CbParts.MANUAL); }
    public static boolean isDateYesterdayPick(String data) { return data != null && Cb.is(data, datePrefix(), CbParts.YESTERDAY); }
    public static boolean isDateErrorBackPick(String data) { return eq(data, errorDateBack()); }
    public static boolean isDateManualBackPick(String data) { return eq(data, manualDateBack()); }

    /** exp:date:yyyy-mm-dd */
    public static String datePick(String isoDate) { return Cb.makeCb(NS, CbParts.DATE, isoDate); }
    /** вернёт yyyy-mm-dd */
    public static String dateIso(String data) { return Cb.tail(data, NS, CbParts.DATE); }

    // ---------------- confirm ----------------

    public static String confirmPrefix() { return ns(CbParts.CONFIRM); }
    public static String confirm(String action) { return actNs(CbParts.CONFIRM, action); }

    public static String confirmCancel() { return confirm(CbParts.CANCEL); }
    public static String confirmBack() { return confirm(CbParts.BACK); }

    public static String getConfirmAction(String data) { return Cb.tail(data, NS, CbParts.CONFIRM); }

    public static boolean isConfirmPick(String data) { return data != null && data.startsWith(confirmPrefix()); }
    public static boolean isConfirmSavePick(String data) { return eq(data, confirm(CbParts.SAVE)); }
    public static boolean isConfirmEditDatePick(String data) { return eq(data, confirm(CbParts.EDIT_DATE)); }
    public static boolean isConfirmEditAmountPick(String data) { return eq(data, confirm(CbParts.EDIT_AMOUNT)); }
    public static boolean isConfirmEditNotePick(String data) { return eq(data, confirm(CbParts.EDIT_NOTE)); }
    public static boolean isConfirmCancelPick(String data) { return eq(data, confirmCancel()); }
    public static boolean isConfirmBackPick(String data) { return eq(data, confirmBack()); }

    // ---------------- receipt ----------------

    public static String receiptPrefix() { return ns(CbParts.RECEIPT); }
    public static boolean isReceipt(String data) { return starts(data, receiptPrefix()); }

    public static String receiptPick(String type) { return Cb.makeCb(NS, CbParts.RECEIPT, type); } // exp:receipt:RECEIPT
    public static String receiptBack() { return back(receiptPrefix()); }                           // exp:receipt:back

    public static boolean isReceiptBackPick(String data) { return eq(data, receiptBack()); }
    public static String receiptType(String data) { return Cb.tail(data, NS, CbParts.RECEIPT); }  // вернёт RECEIPT/...

}
