package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.operation.DocType;
import com.sonumax2.javabot.domain.operation.Operation;
import com.sonumax2.javabot.domain.operation.OperationType;
import com.sonumax2.javabot.domain.operation.repo.ExpenseRepository;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ExpenseSaveService {

    private final OperationRepository operationRepository;
    private final ExpenseRepository expenseRepository; // или ExpenseService (если внутри него upsert)

    public ExpenseSaveService(OperationRepository operationRepository, ExpenseRepository expenseRepository) {
        this.operationRepository = operationRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public ExpenseSaveResult saveExpense(long chatId,
                                         Long objectId,
                                         Long nomenclatureId,
                                         Long counterpartyId,
                                         DocType docType,
                                         BigDecimal amount,
                                         LocalDate opDate,
                                         String note,
                                         String photoFileId) {

        // 1) создаём operation
        Operation op = new Operation();
        op.setChatId(chatId);
        op.setOpType(OperationType.EXP);
        op.setOpDate(opDate);
        op.setAmount(amount);
        op.setNote(note);
        op.setPhotoFileId((docType == DocType.NO_RECEIPT) ? null : photoFileId);
        op.setPhotoFileId(photoFileId);
        op.setCreatedAt(LocalDateTime.now());
        operationRepository.save(op);

        // 2) сохраняем detail (upsert)
        expenseRepository.upsertExpense(
                op.getId(),
                objectId,
                nomenclatureId,
                counterpartyId,
                docType.name()
        );

        // (опционально) вернуть detail или просто id операции
        return new ExpenseSaveResult(op.getId());
    }

    public record ExpenseSaveResult(long operationId) {}
}
