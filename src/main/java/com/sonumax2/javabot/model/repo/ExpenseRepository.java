package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.operation.Expense;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends ListCrudRepository<Expense, Long> {

    List<Expense> findByObjectId(long objectId);
    List<Expense> findByNomenclatureId(long nomenclatureId);
    List<Expense> findByCounterpartyId(long counterpartyId);
    List<Expense> findByObjectIdAndNomenclatureId(long objectId, long nomenclatureId);
    Optional<Expense> findByOperationId(long operationId);
    boolean existsByOperationId(long operationId);

    @Query("""
        select e.nomenclature_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and o.chat_id = :chatId
          and e.nomenclature_id is not null
        group by e.nomenclature_id
        order by max(o.op_date) desc
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
        order by max(o.op_date) desc
        limit :limit
    """)
    List<Long> topNomenclatureIdsOverall(int limit);

    @Query("""
        select e.counterparty_id
        from expense e
        join operation o on o.id = e.operation_id
        where o.is_cancelled = false
          and e.nomenclature_id = :nomId
          and e.counterparty_id is not null
        group by e.counterparty_id
        order by max(o.op_date) desc
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
        order by max(o.op_date) desc
        limit :limit
    """)
    List<Long> topCounterpartyIdsOverall(int limit);
}
