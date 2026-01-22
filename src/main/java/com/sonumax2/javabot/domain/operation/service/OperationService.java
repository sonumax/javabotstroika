package com.sonumax2.javabot.domain.operation.service;

import com.sonumax2.javabot.domain.operation.Operation;
import com.sonumax2.javabot.domain.operation.repo.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OperationService {

    private final OperationRepository repo;

    public OperationService(OperationRepository repo) {
        this.repo = repo;
    }

    public List<Operation> myRecent24h(long chatId) {
        return repo.findTop10ByChatIdAndCancelledFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                chatId, LocalDateTime.now().minusHours(24)
        );
    }

    public Operation requireMyOperation(long chatId, long opId) {
        return repo.findByIdAndChatId(opId, chatId)
                .orElseThrow(() -> new IllegalArgumentException("Operation not found / not yours"));
    }

    public Optional<Operation> myLastActive(long chatId) {
        return repo.findTop1ByChatIdAndCancelledFalseOrderByCreatedAtDesc(chatId);
    }

    @Transactional
    public void cancel(long chatId, long opId) {
        Operation op = requireMyOperation(chatId, opId);
        op.setCancelled(true);
        repo.save(op);
    }
}
