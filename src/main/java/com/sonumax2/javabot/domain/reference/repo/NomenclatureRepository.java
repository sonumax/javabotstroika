package com.sonumax2.javabot.domain.reference.repo;

import com.sonumax2.javabot.domain.reference.Nomenclature;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface NomenclatureRepository extends ListCrudRepository<Nomenclature, Long> {

    List<Nomenclature> findByActiveTrueOrderByNameAsc();
    Optional<Nomenclature> findFirstByActiveTrueAndNameNorm(String nameNorm);
    Optional<Nomenclature> findTop1ByNameNormOrderByIdDesc(String nameNorm);
    List<Nomenclature> findByActiveTrueAndCreatedByChatIdOrderByCreatedAtDesc(Long chatId);

    @Query("""
        select *
        from nomenclature
        where is_active = true
        order by name asc
        limit :limit
    """)
    List<Nomenclature> activeList(int limit);

    @Query("""
        select *
        from nomenclature
        where is_active = true
          and lower(name) like lower(concat('%', :q, '%'))
        order by name asc
        limit :limit
    """)
    List<Nomenclature> searchActiveByName(String q, int limit);

    @Query("""
        select *
        from nomenclature
        where is_active = true
          and created_by_chat_id = :chatId
        order by created_at desc
        limit :limit
    """)
    List<Nomenclature> recentCreatedByChat(Long chatId, int limit);
}
