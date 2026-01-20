package com.sonumax2.javabot.commands.helper;

public class ExpenseCb {
    private ExpenseCb() {}

    public static final String NS = "exp";

    public static String start()      { return Cb.makeCb(CbParts.ADD_OPR, NS); }
    public static boolean isStartPick(String data) { return Cb.is(data, start()); }

    public static String objectPrefix() { return Cb.makeCb(NS, CbParts.OBJ); }
    public static String object(String action)  { return Cb.makeCb(objectPrefix(), action); }
    public static String newObject() { return object(CbParts.NEW); }
    public static String pickObject() { return object(CbParts.PICK); }
    public static String newObjectBack() { return Cb.makeCb(newObject(), CbParts.BACK); }

    public static String getObjectAction(String data)  { return Cb.tail(data, objectPrefix()); }
    public static String getNewObjectId(String data)  { return Cb.tail(data, newObject()); }

    public static boolean isObject (String data) { return Cb.startsWith(data, objectPrefix()); }
    public static boolean isObjectNewPick(String data) { return Cb.is(data, newObject()); }
    public static boolean isObjectPick (String data) { return Cb.is(data, pickObject()); }
    public static boolean isNewObjectBackPick(String data) { return Cb.is(data, newObjectBack()); }

    public static String nomenclaturePrefix() { return Cb.makeCb(NS, CbParts.NOMENCLATURE); }
    public static String nomenclature(String action)  { return Cb.makeCb(nomenclaturePrefix(), action); }
    public static String newNomenclature() { return nomenclature(CbParts.NEW); }
    public static String createNomenclature() { return nomenclature(CbParts.CREATE); }
    public static String searchBackNomenclature() { return Cb.makeCb(nomenclaturePrefix(), CbParts.SEARCH, CbParts.BACK); }
    public static String pickNomenclature() { return nomenclature(CbParts.PICK); }
    public static String nomenclatureBack() { return nomenclature(CbParts.BACK); }
    public static String newNomenclatureBack() { return Cb.makeCb(newNomenclature(), CbParts.BACK); }

    public static long pickNomenclatureId(String data) { return Cb.tailLong(data, pickNomenclature()); }
    public static String nomenclatureAction(String data)  { return Cb.tail(data, nomenclaturePrefix()); }

    public static boolean isNomenclature (String data) { return Cb.startsWith(data, nomenclaturePrefix()); }
    public static boolean isNomenclatureNewPick(String data) { return Cb.is(data, newNomenclature()); }
    public static boolean isNomenclaturePick (String data) { return Cb.is(data, pickNomenclature()); }
    public static boolean isNomenclatureBackPick (String data) { return Cb.is(data, nomenclatureBack()); }
    public static boolean isNewNomenclatureBackPick(String data) { return Cb.is(data, newNomenclatureBack()); }
    public static boolean isCreateNomenclaturePick(String data) { return Cb.is(data, createNomenclature()); }
    public static boolean isSearchBackNomenclaturePick(String data) { return Cb.is(data, searchBackNomenclature()); }

    public static String counterpartyPrefix() { return Cb.makeCb(NS, CbParts.COUNTERPARTY); }
    public static String counterparty(String action)  { return Cb.makeCb(counterpartyPrefix(), action); }
    public static String createCounterparty() { return counterparty(CbParts.CREATE); }
    public static String pickCounterparty() { return counterparty(CbParts.PICK); }
    public static String counterpartyBack() { return counterparty(CbParts.BACK); }
    public static String createCounterpartyBack() { return Cb.makeCb(createCounterparty(), CbParts.BACK); }

    public static String amountPrefix() { return Cb.makeCb(NS, CbParts.AMOUNT); }
    public static String amountBack()   { return Cb.makeCb(amountPrefix(), CbParts.BACK); }
    public static String errorAmountBack() { return Cb.makeCb(amountPrefix(), CbParts.ERR, CbParts.BACK); }
    public static boolean isAmountBackPick(String data) {return Cb.is(data, amountBack()); }
    public static boolean isAmountErrorBackPick(String data) {return Cb.is(data, errorAmountBack()); }

    public static String notePrefix() { return Cb.makeCb(NS, CbParts.NOTE); }
    public static String noteSkip()   { return Cb.makeCb(notePrefix(), CbParts.SKIP); }
    public static String noteBack()   { return Cb.makeCb(notePrefix(), CbParts.BACK); }
    public static boolean isNoteSkipPick(String data) { return Cb.is(data, noteSkip()); }
    public static boolean isNoteBackPick(String data) { return Cb.is(data, noteBack()); }

    public static String counterpartyAction(String data)  { return Cb.tail(data, counterpartyPrefix()); }
    public static long pickCounterpartyId(String data) { return Cb.tailLong(data, pickCounterparty()); }

    public static boolean isCounterparty (String data) { return Cb.startsWith(data, counterpartyPrefix()); }
    public static boolean isCounterpartyCreatePick(String data) { return Cb.is(data, createCounterparty()); }
    public static boolean isCounterpartyPick (String data) { return Cb.startsWith(data, pickCounterparty()); }
    public static boolean isCounterpartyBackPick (String data) { return Cb.is(data, counterpartyBack()); }
    public static boolean isCreateCounterpartyBackPick (String data) { return Cb.is(data, createCounterpartyBack()); }

    public static String datePrefix()   { return Cb.makeCb(NS, CbParts.DATE); }
    public static String errorDateBack() { return Cb.makeCb(datePrefix(), CbParts.ERR, CbParts.BACK); }
    public static String manualDateBack() { return Cb.makeCb(datePrefix(), CbParts.MANUAL, CbParts.BACK);}

    public static boolean isDate(String data) { return Cb.startsWith(data, datePrefix()); }
    public static boolean isDateManualPick(String data) {return Cb.is(data, datePrefix(), CbParts.MANUAL);}
    public static boolean isDateYesterdayPick(String data) {return Cb.is(data, datePrefix(), CbParts.YESTERDAY);}
    public static boolean isDateErrorBackPick(String data) {return Cb.is(data, errorDateBack()); }
    public static boolean isDateManualBackPick(String data) {return Cb.is(data, manualDateBack()); }
    public static String datePick(String isoDate) { return Cb.makeCb(NS, CbParts.DATE, isoDate); }
    public static String dateIso(String data)     { return Cb.tail(data, NS, CbParts.DATE); } // вернёт yyyy-mm-dd

    public static String confirmPrefix()    { return Cb.makeCb(NS, CbParts.CONFIRM); }
    public static String confirm(String action)  { return Cb.makeCb(NS, CbParts.CONFIRM, action); }
    public static String confirmCancel() { return confirm(CbParts.CANCEL);}
    public static String confirmBack() { return confirm(CbParts.BACK);}
    public static String getConfirmAction(String data)  { return Cb.tail(data, NS, CbParts.CONFIRM); }
    public static boolean isConfirmPick(String data) { return data != null && data.startsWith(confirmPrefix()); }
    public static boolean isConfirmSavePick (String data) { return Cb.is(data, confirm(CbParts.SAVE)); }
    public static boolean isConfirmEditDatePick (String data) { return Cb.is(data, confirm(CbParts.EDIT_DATE)); }
    public static boolean isConfirmEditAmountPick (String data) { return Cb.is(data, confirm(CbParts.EDIT_AMOUNT)); }
    public static boolean isConfirmEditNotePick (String data) { return Cb.is(data, confirm(CbParts.EDIT_NOTE)); }
    public static boolean isConfirmCancelPick (String data) { return Cb.is(data, confirmCancel()); }
    public static boolean isConfirmBackPick (String data) { return Cb.is(data, confirmBack()); }

}