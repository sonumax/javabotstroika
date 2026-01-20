package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.operation.Operation;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationRepository extends ListCrudRepository<Operation, Long> {

    List<Operation> findTop10ByChatIdAndCancelledFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            Long chatId, LocalDateTime since
    );

    Optional<Operation> findByIdAndChatId(Long id, Long chatId);

    Optional<Operation> findTop1ByChatIdAndCancelledFalseOrderByCreatedAtDesc(Long chatId);
}
