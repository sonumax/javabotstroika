package com.sonumax2.javabot.domain.reference.repo;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends ListCrudRepository<Equipment, Long> {

    @Query("""
        select *
        from equipment
        where chat_id = :chatId and is_active = true
        order by name asc
        limit :limit
    """)
    List<Equipment> listActiveTop(long chatId, int limit);

    @Query("""
        select *
        from equipment
        where chat_id = :chatId and is_active = true
          and name_norm like concat('%', :q, '%')
        order by name asc
        limit :limit
    """)
    List<Equipment> searchActive(long chatId, String q, int limit);

    @Query("""
        select *
        from equipment
        where chat_id = :chatId and name_norm = :nameNorm
        limit 1
    """)
    Optional<Equipment> findByChatIdAndNameNorm(long chatId, String nameNorm);
}
