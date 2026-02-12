package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.draft.ExpenseLineItem;
import com.sonumax2.javabot.domain.draft.SypDraft;
import com.sonumax2.javabot.domain.operation.*;
import com.sonumax2.javabot.domain.operation.repo.SypItemRepository;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import com.sonumax2.javabot.domain.reference.service.NomenclatureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SypService {

    private final OperationRepository operationRepo;
    private final SypItemRepository itemRepo;
    private final NomenclatureService nomenclatureService;

    public SypService(OperationRepository operationRepo,
                      SypItemRepository itemRepo,
                      NomenclatureService nomenclatureService) {
        this.operationRepo = operationRepo;
        this.itemRepo = itemRepo;
        this.nomenclatureService = nomenclatureService;
    }

    @Transactional
    public long save(SypDraft d, long chatId) {
        Objects.requireNonNull(d, "draft");
        validate(d);

        DocType dt = (d.docType == null) ? DocType.NO_RECEIPT : d.docType;

        Operation op = new Operation();
        op.setCreatedAt(LocalDateTime.now());
        op.setChatId(chatId);
        op.setOpType(OperationType.SYP);

        op.setAmount(d.amount);
        op.setOpDate(d.date);
        op.setNote(d.note);

        // фото хранится в operation
        op.setPhotoFileId(dt == DocType.NO_RECEIPT ? null : d.docFileId);

        SypDetail sypDetail = new SypDetail();
        sypDetail.setWorkObjectId(d.objectId);
        sypDetail.setCounterpartyId(d.counterpartyId);
        sypDetail.setPayType(d.payType);
        sypDetail.setDocType(dt);

        op.getSypDetails().clear();
        op.getSypDetails().add(sypDetail);

        op = operationRepo.save(op);

        // items: сначала очистить (на случай редактирования/повторного save)
        itemRepo.deleteByOperationId(op.getId());

        for (ExpenseLineItem it : d.items) {
            if (it == null || it.nomenclatureId == null || it.volume == null) continue;
            itemRepo.insertOne(op.getId(), it.nomenclatureId, it.volume);
        }

        Set<Long> ids = d.items.stream()
                .map(it -> it.nomenclatureId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        nomenclatureService.markUsedInSyp(ids);

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
