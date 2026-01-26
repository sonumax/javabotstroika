package com.sonumax2.javabot.domain.reference.repo;

import com.sonumax2.javabot.domain.reference.WorkObject;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface WorkObjectRepository extends ListCrudRepository<WorkObject, Long> {

    List<WorkObject> findByActiveTrueOrderByNameAsc();
    Optional<WorkObject> findFirstByActiveTrueAndNameNorm(String nameNorm);
    Optional<WorkObject> findTop1ByNameNormOrderByIdDesc(String nameNorm);

    @Query("""
        select *
        from work_object
        where is_active = true
        order by name asc
        limit :limit
    """)
    List<WorkObject> activeList(int limit);

    @Query("""
        select *
        from work_object
        where is_active = true
          and name_norm like concat('%', :q, '%')
        order by name asc
        limit :limit
    """)
    List<WorkObject> searchActiveByName(String q, int limit);

    @Query("""
        select *
        from work_object
        where is_active = true
          and created_by_chat_id = :chatId
        order by created_at desc
        limit :limit
    """)
    List<WorkObject> recentCreatedByChat(Long chatId, int limit);
}
