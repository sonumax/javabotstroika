package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.draft.FuelDraft;
import com.sonumax2.javabot.domain.draft.FuelKind;
import com.sonumax2.javabot.domain.operation.*;
import com.sonumax2.javabot.domain.operation.repo.FuelDetailRepository;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class FuelService {

    private final OperationRepository operationRepo;
    private final FuelDetailRepository detailRepo;

    public FuelService(OperationRepository operationRepo, FuelDetailRepository detailRepo) {
        this.operationRepo = operationRepo;
        this.detailRepo = detailRepo;
    }

    @Transactional
    public long save(FuelDraft d, long chatId) {
        Objects.requireNonNull(d, "draft");
        validate(d);

        DocType dt = (d.docType == null) ? DocType.NO_RECEIPT : d.docType;

        OperationType opType = (d.fuelKind == FuelKind.MACHINE)
                ? OperationType.FUEL_MACHINE
                : OperationType.FUEL_TRANSPORT;

        Operation op = new Operation();
        op.setChatId(chatId);
        op.setOpType(opType);

        op.setAmount(d.amount);
        op.setOpDate(d.date);
        op.setNote(d.note);

        op.setPhotoFileId(dt == DocType.NO_RECEIPT ? null : d.docFileId);

        op = operationRepo.save(op);

        FuelDetail detail = new FuelDetail();
        detail.setOperationId(op.getId());
        detail.setFuelKind(d.fuelKind);
        detail.setEquipmentId(d.equipmentId);
        detail.setCounterpartyId(d.counterpartyId);
        detail.setVolume(d.volume);
        detail.setDocType(dt);

        detailRepo.save(detail);

        return op.getId();
    }

    private void validate(FuelDraft d) {
        if (d.fuelKind == null) throw new IllegalArgumentException("fuelKind is required");
        if (d.equipmentId == null) throw new IllegalArgumentException("equipment is required");
        if (d.counterpartyId == null) throw new IllegalArgumentException("counterparty is required");
        if (d.volume == null) throw new IllegalArgumentException("volume is required");
        if (d.amount == null) throw new IllegalArgumentException("amount is required");
        if (d.date == null) throw new IllegalArgumentException("date is required");
    }
}
