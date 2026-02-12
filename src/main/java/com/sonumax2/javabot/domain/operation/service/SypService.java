package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.draft.SypDraft;
import com.sonumax2.javabot.domain.operation.*;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SypService implements OperationSaver<SypDraft> {

    private final OperationRepository operationRepo;

    public SypService(OperationRepository operationRepo) {
        this.operationRepo = operationRepo;
    }

    @Transactional
    public long save(SypDraft d, long chatId) {
        Operation op = OperationFactory.fromSyp(d);
        op.setChatId(chatId);
        op = operationRepo.save(op);
        return op.getId();
    }

    private void validate(SypDraft d) {
        if (d.objectId == null) throw new IllegalArgumentException("work object is required");
        if (d.counterpartyId == null) throw new IllegalArgumentException("counterparty is required");
        if (d.payType == null) throw new IllegalArgumentException("payType is required");
        if (d.amount == null) throw new IllegalArgumentException("amount is required");
        if (d.date == null) throw new IllegalArgumentException("date is required");
        if (d.items == null || d.items.isEmpty()) throw new IllegalArgumentException("items are required");
    }
}
