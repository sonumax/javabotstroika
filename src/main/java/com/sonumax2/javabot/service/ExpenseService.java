package com.sonumax2.javabot.service;

import com.sonumax2.javabot.model.operation.Expense;
import com.sonumax2.javabot.model.reference.Counterparty;
import com.sonumax2.javabot.model.repo.CounterpartyRepository;
import com.sonumax2.javabot.model.repo.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final CounterpartyRepository counterpartyRepo;

    public ExpenseService(ExpenseRepository expenseRepo, CounterpartyRepository counterpartyRepo) {
        this.expenseRepo = expenseRepo;
        this.counterpartyRepo = counterpartyRepo;
    }

    public Optional<Expense> findByOperationId(long operationId) {
        return expenseRepo.findByOperationId(operationId);
    }

    @Transactional
    public Expense saveDetail(long operationId, long objectId, long nomenclatureId, Long counterpartyId) {
        Expense e = new Expense(operationId, objectId, nomenclatureId, counterpartyId);
        return expenseRepo.save(e);
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
            ids.add(id);
            if (ids.size() >= limit) break;
        }

        if (ids.size() < limit) {
            for (Long id : expenseRepo.topNomenclatureIdsOverall(limit)) {
                ids.add(id);
                if (ids.size() >= limit) break;
            }
        }

        return new ArrayList<>(ids);
    }


    public List<Counterparty> suggestSuppliers(long nomId, int limit) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();

        for (Long id : expenseRepo.topCounterpartyIdsByNomenclature(nomId, limit)) {
            ids.add(id);
            if (ids.size() >= limit) break;
        }

        if (ids.size() < limit) {
            for (Long id : expenseRepo.topCounterpartyIdsOverall(limit)) {
                ids.add(id);
                if (ids.size() >= limit) break;
            }
        }

        return loadCounterpartiesInOrder(ids);
    }

    private List<Counterparty> loadCounterpartiesInOrder(LinkedHashSet<Long> ids) {
        if (ids.isEmpty()) return List.of();

        List<Counterparty> cps = StreamSupport
                .stream(counterpartyRepo.findAllById(ids).spliterator(), false)
                .filter(Counterparty::isActive)
                .toList();

        Map<Long, Counterparty> map = new HashMap<>();
        for (Counterparty c : cps) map.put(c.getId(), c);

        List<Counterparty> ordered = new ArrayList<>();
        for (Long id : ids) {
            Counterparty c = map.get(id);
            if (c != null) ordered.add(c);
        }
        return ordered;
    }
}
