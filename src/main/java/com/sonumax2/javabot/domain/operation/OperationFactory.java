package com.sonumax2.javabot.domain.operation;

import com.sonumax2.javabot.domain.draft.*;
import java.util.ArrayList;

public class OperationFactory {

    public static Operation fromFuel(FuelDraft d) {
        Operation op = base(OperationType.FUEL, d.amount, d.date, d.note);

        FuelDetail detail = new FuelDetail();
        detail.setFuelKind(d.fuelKind);
        detail.setObjectId(d.objectId);
        detail.setMachineTypeId(d.isMachine() ? d.machineTypeId : null);
        detail.setCounterpartyId(d.counterpartyId);
        detail.setVolume(d.volume);
        detail.setDocType(d.docType);

        op.setFuelDetail(detail);
        return op;
    }

    public static Operation fromSyp(SypDraft d) {
        Operation op = base(OperationType.SYP, d.amount, d.date, d.note);

        SypDetail detail = new SypDetail();
        detail.setWorkObjectId(d.objectId);
        detail.setCounterpartyId(d.counterpartyId);
        detail.setPayType(d.payType);
        detail.setDocType(d.docType);

        op.setSypDetail(detail);

        var items = new java.util.ArrayList<SypItem>();
        for (var li : d.items) {
            SypItem item = new SypItem();
            item.setNomenclatureId(li.nomenclatureId);
            item.setVolume(li.volume);
            items.add(item);
        }
        op.replaceSypItems(items);

        return op;
    }

    private static Operation base(
            OperationType type,
            java.math.BigDecimal amount,
            java.time.LocalDate date,
            String note
    ) {
        Operation op = new Operation();
        op.setOpType(type);
        op.setAmount(amount);
        op.setOpDate(date);
        op.setNote(note);
        return op;
    }
}
