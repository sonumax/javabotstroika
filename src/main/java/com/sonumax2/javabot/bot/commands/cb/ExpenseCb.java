package com.sonumax2.javabot.bot.commands.cb;

public class ExpenseCb {
    private ExpenseCb() {}

    public static final String NS = "exp";

    // ---------- helpers (единые правила) ----------

    /** exp:<part> */
    private static String prefix(String part) { return Cb.makeCb(NS, part); }

    /** exp:<part>:<action> */
    private static String cb(String part, String action) { return Cb.makeCb(NS, part, action); }

    /** <prefix>:back (prefix может быть exp:<part> или exp:<part>:new и т.п.) */
    private static String back(String anyPrefix) { return Cb.makeCb(anyPrefix, CbParts.BACK); }

    private static boolean eq(String data, String expected) { return Cb.is(data, expected); }
    private static boolean starts(String data, String expectedPrefix) { return Cb.startsWith(data, expectedPrefix); }

    // ---------------- start ----------------

    /** add_opr:exp */
    public static String start() { return Cb.makeCb(CbParts.ADD_OPR, NS); }
    public static boolean isStartPick(String data) { return eq(data, start()); }

    // ---------------- object ----------------
    // exp:obj:...

    public static String objectPrefix() { return prefix(CbParts.OBJ); }
    public static boolean isObject(String data) { return starts(data, objectPrefix()); }

    /** exp:obj:new */
    public static String newObject() { return cb(CbParts.OBJ, CbParts.NEW); }
    /** exp:obj:new:back */
    public static String newObjectBack() { return back(newObject()); }
    public static boolean isObjectNewPick(String data) { return eq(data, newObject()); }
    public static boolean isNewObjectBackPick(String data) { return eq(data, newObjectBack()); }

    /** exp:obj:pick  (префикс для exp:obj:pick:<id>) */
    public static String pickObject() { return cb(CbParts.OBJ, CbParts.PICK); }
    /** exp:obj:pick:<id> */
    public static boolean isObjectPick(String data) { return starts(data, pickObject()); }
    public static long pickObjectId(String data) { return Cb.tailLong(data, pickObject()); }

    public static String getObjectAction(String data) { return Cb.tail(data, objectPrefix()); }
    public static long getNewObjectId(String data) { return Cb.tailLong(data, newObject()); }

    // ---------------- nomenclature ----------------
    // exp:nomenclature:...

    public static String nomenclaturePrefix() { return prefix(CbParts.NOMENCLATURE); }
    public static boolean isNomenclature(String data) { return starts(data, nomenclaturePrefix()); }

    /** exp:nomenclature:back */
    public static String nomenclatureBack() { return back(nomenclaturePrefix()); }
    public static boolean isNomenclatureBackPick(String data) { return eq(data, nomenclatureBack()); }

    /** exp:nomenclature:new */
    public static String newNomenclature() { return cb(CbParts.NOMENCLATURE, CbParts.NEW); }
    /** exp:nomenclature:new:back */
    public static String newNomenclatureBack() { return back(newNomenclature()); }
    public static boolean isNomenclatureNewPick(String data) { return eq(data, newNomenclature()); }
    public static boolean isNewNomenclatureBackPick(String data) { return eq(data, newNomenclatureBack()); }

    /** exp:nomenclature:create */
    public static String createNomenclature() { return cb(CbParts.NOMENCLATURE, CbParts.CREATE); }
    public static boolean isCreateNomenclaturePick(String data) { return eq(data, createNomenclature()); }

    /** exp:nomenclature:search:back */
    public static String searchBackNomenclature() {
        return Cb.makeCb(nomenclaturePrefix(), CbParts.SEARCH, CbParts.BACK);
    }
    public static boolean isSearchBackNomenclaturePick(String data) { return eq(data, searchBackNomenclature()); }

    /** exp:nomenclature:pick (префикс для exp:nomenclature:pick:<id>) */
    public static String pickNomenclature() { return cb(CbParts.NOMENCLATURE, CbParts.PICK); }
    /** exp:nomenclature:pick:<id> */
    public static boolean isNomenclaturePick(String data) { return starts(data, pickNomenclature()); }
    public static long pickNomenclatureId(String data) { return Cb.tailLong(data, pickNomenclature()); }

    public static String nomenclatureAction(String data) { return Cb.tail(data, nomenclaturePrefix()); }

    // ---------------- counterparty ----------------
    // exp:counterparty:...

    public static String counterpartyPrefix() { return prefix(CbParts.COUNTERPARTY); }
    public static boolean isCounterparty(String data) { return starts(data, counterpartyPrefix()); }

    /** exp:counterparty:back */
    public static String counterpartyBack() { return back(counterpartyPrefix()); }
    public static boolean isCounterpartyBackPick(String data) { return eq(data, counterpartyBack()); }

    /** exp:counterparty:new */
    public static String newCounterparty() { return cb(CbParts.COUNTERPARTY, CbParts.NEW); }
    /** exp:counterparty:new:back */
    public static String newCounterpartyBack() { return back(newCounterparty()); }
    public static boolean isCounterpartyNewPick(String data) { return eq(data, newCounterparty()); }
    public static boolean isNewCounterpartyBackPick(String data) { return eq(data, newCounterpartyBack()); }

    /** exp:counterparty:skip  (пропустить продавца) */
    public static String skipCounterparty() { return cb(CbParts.COUNTERPARTY, CbParts.SKIP); }
    public static boolean isCounterpartySkipPick(String data) { return eq(data, skipCounterparty()); }

    /** exp:counterparty:pick (префикс для exp:counterparty:pick:<id>) */
    public static String pickCounterparty() { return cb(CbParts.COUNTERPARTY, CbParts.PICK); }
    /** exp:counterparty:pick:<id> */
    public static boolean isCounterpartyPick(String data) { return starts(data, pickCounterparty()); }
    public static long pickCounterpartyId(String data) { return Cb.tailLong(data, pickCounterparty()); }

    /** exp:counterparty:create */
    public static String createCounterparty() { return cb(CbParts.COUNTERPARTY, CbParts.CREATE); }
    /** exp:counterparty:create:back */
    public static String createCounterpartyBack() { return back(createCounterparty()); }
    public static boolean isCounterpartyCreatePick(String data) { return eq(data, createCounterparty()); }
    public static boolean isCreateCounterpartyBackPick(String data) { return eq(data, createCounterpartyBack()); }

    /** exp:counterparty:search:back */
    public static String searchBackCounterparty() {
        return Cb.makeCb(counterpartyPrefix(), CbParts.SEARCH, CbParts.BACK);
    }
    public static boolean isSearchBackCounterpartyPick(String data) { return eq(data, searchBackCounterparty()); }


    public static String counterpartyAction(String data) { return Cb.tail(data, counterpartyPrefix()); }

    // ---------------- amount ----------------
    // exp:amount:...

    public static String amountPrefix() { return prefix(CbParts.AMOUNT); }

    /** exp:amount:back */
    public static String amountBack() { return back(amountPrefix()); }
    /** exp:amount:err:back */
    public static String errorAmountBack() { return Cb.makeCb(amountPrefix(), CbParts.ERR, CbParts.BACK); }

    public static boolean isAmountBackPick(String data) { return eq(data, amountBack()); }
    public static boolean isAmountErrorBackPick(String data) { return eq(data, errorAmountBack()); }

    // ---------------- note ----------------
    // exp:note:...

    public static String notePrefix() { return prefix(CbParts.NOTE); }

    /** exp:note:skip */
    public static String noteSkip() { return cb(CbParts.NOTE, CbParts.SKIP); }
    /** exp:note:back */
    public static String noteBack() { return back(notePrefix()); }

    public static boolean isNoteSkipPick(String data) { return eq(data, noteSkip()); }
    public static boolean isNoteBackPick(String data) { return eq(data, noteBack()); }

    // ---------------- date ----------------
    // exp:date:...

    public static String datePrefix() { return prefix(CbParts.DATE); }
    public static boolean isDate(String data) { return starts(data, datePrefix()); }

    /** exp:date:err:back */
    public static String errorDateBack() { return Cb.makeCb(datePrefix(), CbParts.ERR, CbParts.BACK); }
    /** exp:date:manual:back */
    public static String manualDateBack() { return Cb.makeCb(datePrefix(), CbParts.MANUAL, CbParts.BACK); }

    public static boolean isDateManualPick(String data) { return data != null && Cb.is(data, datePrefix(), CbParts.MANUAL); }
    public static boolean isDateYesterdayPick(String data) { return data != null && Cb.is(data, datePrefix(), CbParts.YESTERDAY); }
    public static boolean isDateErrorBackPick(String data) { return eq(data, errorDateBack()); }
    public static boolean isDateManualBackPick(String data) { return eq(data, manualDateBack()); }

    /** exp:date:yyyy-mm-dd */
    public static String datePick(String isoDate) { return Cb.makeCb(NS, CbParts.DATE, isoDate); }
    /** вернёт yyyy-mm-dd */
    public static String dateIso(String data) { return Cb.tail(data, NS, CbParts.DATE); }

    // ---------------- confirm ----------------
    // exp:confirm:...

    public static String confirmPrefix() { return prefix(CbParts.CONFIRM); }
    public static boolean isConfirmPick(String data) { return starts(data, confirmPrefix()); }

    /** exp:confirm:<action> */
    private static String confirm(String action) { return cb(CbParts.CONFIRM, action); }

    public static String confirmCancel() { return confirm(CbParts.CANCEL); }
    public static String confirmBack() { return confirm(CbParts.BACK); }

    public static boolean isConfirmSavePick(String data) { return eq(data, confirm(CbParts.SAVE)); }
    public static boolean isConfirmEditDatePick(String data) { return eq(data, confirm(CbParts.EDIT_DATE)); }
    public static boolean isConfirmEditAmountPick(String data) { return eq(data, confirm(CbParts.EDIT_AMOUNT)); }
    public static boolean isConfirmEditNotePick(String data) { return eq(data, confirm(CbParts.EDIT_NOTE)); }
    public static boolean isConfirmCancelPick(String data) { return eq(data, confirmCancel()); }
    public static boolean isConfirmBackPick(String data) { return eq(data, confirmBack()); }

    public static String getConfirmAction(String data) { return Cb.tail(data, NS, CbParts.CONFIRM); }

    // ---------------- receipt ----------------
    // exp:receipt:...

    public static String receiptPrefix() { return prefix(CbParts.RECEIPT); }
    public static boolean isReceipt(String data) { return starts(data, receiptPrefix()); }

    /** exp:receipt:<type> */
    public static String receiptPick(String type) { return Cb.makeCb(NS, CbParts.RECEIPT, type); }
    /** exp:receipt:back */
    public static String receiptBack() { return back(receiptPrefix()); }

    public static boolean isReceiptBackPick(String data) { return eq(data, receiptBack()); }
    public static String receiptType(String data) { return Cb.tail(data, NS, CbParts.RECEIPT); }
}
