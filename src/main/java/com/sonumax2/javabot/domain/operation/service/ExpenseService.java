package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.draft.service.DraftService;
import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.Expense;
import com.sonumax2.javabot.domain.operation.Operation;
import com.sonumax2.javabot.domain.operation.OperationType;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import com.sonumax2.javabot.domain.reference.Counterparty;
import com.sonumax2.javabot.domain.operation.repo.ExpenseRepository;
import com.sonumax2.javabot.domain.reference.service.CounterpartyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final CounterpartyService counterpartyService;
    private final OperationRepository operationRepository;
    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepo, CounterpartyService counterpartyService, OperationRepository operationRepository, ExpenseRepository expenseRepository) {
        this.expenseRepo = expenseRepo;
        this.counterpartyService = counterpartyService;
        this.operationRepository = operationRepository;
        this.expenseRepository = expenseRepository;
    }

    public Optional<Expense> findByOperationId(long operationId) {
        return expenseRepo.findByOperationId(operationId);
    }

    @Transactional
    public Expense saveDetail(long operationId,
                              long objectId,
                              long nomenclatureId,
                              Long counterpartyId,
                              DocType docType) {

        DocType rt = (docType == null ? DocType.NO_RECEIPT : docType);
        expenseRepo.upsertExpense(operationId, objectId, nomenclatureId, counterpartyId, rt.name());

        return expenseRepo.findByOperationId(operationId)
                .orElseThrow(() -> new IllegalStateException("Expense not saved for operationId=" + operationId));
    }

    @Transactional
    public void saveExpense(long chatId,
                                                            Long objectId,
                                                            Long nomenclatureId,
                                                            Long counterpartyId,
                                                            DocType docType,
                                                            BigDecimal amount,
                                                            LocalDate opDate,
                                                            String note,
                                                            String photoFileId) {

        DocType dt = (docType == null ? DocType.NO_RECEIPT : docType);

        // 1) создаём operation
        Operation op = new Operation();
        op.setChatId(chatId);
        op.setOpType(OperationType.EXP);
        op.setOpDate(opDate);
        op.setAmount(amount);
        op.setNote(note);
        op.setPhotoFileId(dt == DocType.NO_RECEIPT ? null : photoFileId);
        op.setCreatedAt(LocalDateTime.now());
        operationRepository.save(op);

        // 2) сохраняем detail (upsert)
        expenseRepository.upsertExpense(
                op.getId(),
                objectId,
                nomenclatureId,
                counterpartyId,
                dt.name()
        );
    }

    @Transactional
    public Expense setCounterparty(long operationId, Long counterpartyId) {
        Expense e = expenseRepo.findByOperationId(operationId)
                .orElseThrow(() -> new IllegalStateException("Expense not found for operation_id=" + operationId));
        e.setCounterpartyId(counterpartyId);
        return expenseRepo.save(e);
    }

    public List<Expense> listByObject(long objectId) {
        return expenseRepo.findByObjectId(objectId);
    }

    public List<Expense> listByNomenclature(long nomenclatureId) {
        return expenseRepo.findByNomenclatureId(nomenclatureId);
    }

    public List<Expense> listByCounterparty(long counterpartyId) {
        return expenseRepo.findByCounterpartyId(counterpartyId);
    }

    public List<Long> suggestNomenclatureIds(long chatId, int limit) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();

        for (Long id : expenseRepo.topNomenclatureIdsByChat(chatId, limit)) {
            if (id == null) continue;
            ids.add(id);
            if (ids.size() >= limit) break;
        }

        if (ids.size() < limit) {
            for (Long id : expenseRepo.topNomenclatureIdsOverall(limit)) {
                if (id == null) continue;
                ids.add(id);
                if (ids.size() >= limit) break;
            }
        }

        return new ArrayList<>(ids);
    }


    public List<Counterparty> suggestCounterparty(long chatId, long nomId, int limit) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();

        for (Long id : expenseRepo.topCounterpartyIdsByChatAndNomenclature(chatId, nomId, limit)) {
            if (id == null) continue;
            ids.add(id);
            if (ids.size() >= limit) break;
        }

        if (ids.size() < limit) {
            for (Long id : expenseRepo.topCounterpartyIdsByNomenclature(nomId, limit)) {
                if (id == null) continue;
                ids.add(id);
                if (ids.size() >= limit) break;
            }
        }

        if (ids.size() < limit) {
            for (Long id : expenseRepo.topCounterpartyIdsOverall(limit)) {
                if (id == null) continue;
                ids.add(id);
                if (ids.size() >= limit) break;
            }
        }

        return loadCounterpartiesInOrder(new ArrayList<>(ids));
    }


    private List<Counterparty> loadCounterpartiesInOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<Counterparty> items = StreamSupport
                .stream(counterpartyService.findAllById(ids).spliterator(), false)
                .filter(Counterparty::isActive)
                .toList();

        Map<Long, Counterparty> byId = new HashMap<>();
        for (Counterparty c : items) byId.put(c.getId(), c);

        List<Counterparty> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Counterparty c = byId.get(id);
            if (c != null) ordered.add(c);
        }
        return ordered;
    }
}
