package com.sonumax2.javabot.domain.operation.repo;

import com.sonumax2.javabot.domain.operation.Expense;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends ListCrudRepository<Expense, Long> {

    // convenient finders
    Optional<Expense> findByOperationId(long operationId);
    boolean existsByOperationId(long operationId);

    List<Expense> findByObjectId(long objectId);
    List<Expense> findByNomenclatureId(long nomenclatureId);
    List<Expense> findByCounterpartyId(long counterpartyId);
    List<Expense> findByObjectIdAndNomenclatureId(long objectId, long nomenclatureId);

    /**
     * Suggest Nomenclature: first by chat (most used), then fallback to overall.
     * Sorting: count desc (top used) + last date desc (freshness).
     */
    @Query("""
        select e.nomenclature_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and o.chat_id = :chatId
          and e.nomenclature_id is not null
        group by e.nomenclature_id
        order by count(*) desc, max(o.op_date) desc
        limit :limit
    """)
    List<Long> topNomenclatureIdsByChat(long chatId, int limit);

    @Query("""
        select e.nomenclature_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and e.nomenclature_id is not null
        group by e.nomenclature_id
        order by count(*) desc, max(o.op_date) desc
        limit :limit
    """)
    List<Long> topNomenclatureIdsOverall(int limit);

    /**
     * Suggest Counterparty for конкретной номенклатуры (то, что ты спрашивал).
     * Sorting: count desc + свежесть.
     */
    @Query("""
        select e.counterparty_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and e.nomenclature_id = :nomId
          and e.counterparty_id is not null
        group by e.counterparty_id
        order by count(*) desc, max(o.op_date) desc
        limit :limit
    """)
    List<Long> topCounterpartyIdsByNomenclature(long nomId, int limit);

    @Query("""
        select e.counterparty_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and e.counterparty_id is not null
        group by e.counterparty_id
        order by count(*) desc, max(o.op_date) desc
        limit :limit
    """)
    List<Long> topCounterpartyIdsOverall(int limit);

    /**
     * персонализировать контрагентов под чат:
     * сначала (chatId + nomId), потом (nomId overall), потом overall.
     */
    @Query("""
        select e.counterparty_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and o.chat_id = :chatId
          and e.nomenclature_id = :nomId
          and e.counterparty_id is not null
        group by e.counterparty_id
        order by count(*) desc, max(o.op_date) desc
        limit :limit
    """)
    List<Long> topCounterpartyIdsByChatAndNomenclature(long chatId, long nomId, int limit);
}
