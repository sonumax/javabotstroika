package com.sonumax2.javabot.domain.operation.repo;

import com.sonumax2.javabot.domain.operation.Operation;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationRepository extends ListCrudRepository<Operation, Long> {

    List<Operation> findTop10ByChatIdAndIsCancelledFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            long chatId, LocalDateTime since
    );
    Optional<Operation> findByIdAndChatId(long id, long chatId);
    Optional<Operation> findTop1ByChatIdAndIsCancelledFalseOrderByCreatedAtDesc(long chatId);
}
