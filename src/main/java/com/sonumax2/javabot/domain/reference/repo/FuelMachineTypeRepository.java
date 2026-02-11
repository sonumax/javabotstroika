package com.sonumax2.javabot.domain.reference.repo;

import com.sonumax2.javabot.domain.reference.FuelMachineType;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface FuelMachineTypeRepository extends ListCrudRepository<FuelMachineType, Long> {

    List<FuelMachineType> findByActiveTrueOrderByNameAsc();
    Optional<FuelMachineType> findFirstByActiveTrueAndNameNorm(String nameNorm);
    Optional<FuelMachineType> findTop1ByNameNormOrderByIdDesc(String nameNorm);

    @Query("""
        select *
        from fuel_machine_type
        where is_active = true
        order by name asc
        limit :limit
    """)
    List<FuelMachineType> activeList(int limit);

    @Query("""
        select *
        from fuel_machine_type
        where is_active = true
          and name_norm like concat('%', :q, '%')
        order by name asc
        limit :limit
    """)
    List<FuelMachineType> searchActiveByName(String q, int limit);

    @Query("""
        select *
        from fuel_machine_type
        where is_active = true
          and created_by_chat_id = :chatId
        order by created_at desc
        limit :limit
    """)
    List<FuelMachineType> recentCreatedByChat(Long chatId, int limit);
}
