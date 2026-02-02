package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.draft.SypDraft;
import com.sonumax2.javabot.domain.operation.*;
import com.sonumax2.javabot.domain.operation.repo.SypDetailRepository;
import com.sonumax2.javabot.domain.operation.repo.SypItemRepository;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class SypService {

    private final OperationRepository operationRepo;
    private final SypDetailRepository detailRepo;
    private final SypItemRepository itemRepo;

    public SypService(OperationRepository operationRepo,
                      SypDetailRepository detailRepo,
                      SypItemRepository itemRepo) {
        this.operationRepo = operationRepo;
        this.detailRepo = detailRepo;
        this.itemRepo = itemRepo;
    }

    @Transactional
    public long save(SypDraft d, long chatId) {
        Objects.requireNonNull(d, "draft");
        validate(d);

        DocType dt = (d.docType == null) ? DocType.NO_RECEIPT : d.docType;

        Operation op = new Operation();
        op.setChatId(chatId);
        op.setOpType(OperationType.SYP);

        op.setAmount(d.amount);
        op.setOpDate(d.date);
        op.setNote(d.note);

        // фото хранится в operation
        op.setPhotoFileId(dt == DocType.NO_RECEIPT ? null : d.docFileId);

        op = operationRepo.save(op);

        // detail (docType хранится тут)
        SypDetail detail = new SypDetail();
        detail.setOperationId(op.getId());
        detail.setWorkObjectId(d.objectId);
        detail.setCounterpartyId(d.counterpartyId);
        detail.setPayType(d.payType);
        detail.setDocType(dt);
        detailRepo.save(detail);

        // items: сначала очистить (на случай редактирования/повторного save)
        itemRepo.deleteByOperationId(op.getId());

        for (SypDraft.SypItem it : d.items) {
            if (it == null) continue;
            if (it.nomenclatureId == null) continue;
            if (it.volume == null) continue;

            itemRepo.insertOne(op.getId(), it.nomenclatureId, it.volume);
        }

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
