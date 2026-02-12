package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.draft.FuelDraft;
import com.sonumax2.javabot.domain.operation.*;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuelService implements OperationSaver<FuelDraft> {

    private final OperationRepository operationRepo;

    public FuelService(OperationRepository operationRepo) { this.operationRepo = operationRepo; }

    @Transactional
    public long save(FuelDraft d, long chatId) {
        Operation op = OperationFactory.fromFuel(d);
        op.setChatId(chatId);
        op = operationRepo.save(op);
        return op.getId();
    }

    private void validate(FuelDraft d) {
        if (d.fuelKind == null) throw new IllegalArgumentException("fuelKind is required");
        if (d.isMachine() && d.machineTypeId == null) throw new IllegalArgumentException("machineTypeId is required");
        if (d.objectId == null) throw new IllegalArgumentException("object is required");
        if (d.counterpartyId == null) throw new IllegalArgumentException("counterparty is required");
        if (d.volume == null) throw new IllegalArgumentException("volume is required");
        if (d.amount == null) throw new IllegalArgumentException("amount is required");
        if (d.date == null) throw new IllegalArgumentException("date is required");
    }
}
