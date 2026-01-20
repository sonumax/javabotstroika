package com.sonumax2.javabot.model.repo;

import com.sonumax2.javabot.model.reference.Counterparty;
import com.sonumax2.javabot.model.reference.CounterpartyKind;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface CounterpartyRepository extends ListCrudRepository<Counterparty, Long> {

    // --- simple derived queries (без пагинации) ---
    List<Counterparty> findByActiveTrueOrderByNameAsc();

    List<Counterparty> findByActiveTrueAndKindOrderByNameAsc(CounterpartyKind kind);

    Optional<Counterparty> findFirstByActiveTrueAndNameNorm(String nameNorm);

    Optional<Counterparty> findFirstByActiveTrueAndKindAndNameNorm(CounterpartyKind kind, String nameNorm);

    // для "реактивации" по nameNorm (если используешь)
    Optional<Counterparty> findTop1ByNameNormOrderByIdDesc(String nameNorm);

    List<Counterparty> findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(Long chatId);

    // --- limited lists / search (вместо Pageable) ---

    @Query("""
        select *
        from counterparty
        where is_active = true
        order by name asc
        limit :limit
    """)
    List<Counterparty> activeList(int limit);

    @Query("""
        select *
        from counterparty
        where is_active = true
          and kind = :kind
        order by name asc
        limit :limit
    """)
    List<Counterparty> activeListByKind(CounterpartyKind kind, int limit);

    @Query("""
        select *
        from counterparty
        where is_active = true
          and lower(name) like lower(concat('%', :q, '%'))
        order by name asc
        limit :limit
    """)
    List<Counterparty> searchActiveByName(String q, int limit);

    @Query("""
        select *
        from counterparty
        where is_active = true
          and kind = :kind
          and lower(name) like lower(concat('%', :q, '%'))
        order by name asc
        limit :limit
    """)
    List<Counterparty> searchActiveByKindAndName(CounterpartyKind kind, String q, int limit);

    @Query("""
        select *
        from counterparty
        where is_active = true
          and created_by_chat_id = :chatId
        order by created_at desc
        limit :limit
    """)
    List<Counterparty> recentCreatedByChat(Long chatId, int limit);
}
