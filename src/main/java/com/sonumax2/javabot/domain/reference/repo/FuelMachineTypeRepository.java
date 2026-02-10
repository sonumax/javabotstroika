package com.sonumax2.javabot.domain.reference.repo;

import com.sonumax2.javabot.domain.reference.FuelMachineType;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface FuelMachineTypeRepository extends CrudRepository<FuelMachineType, Long> {

    @Query("""
        select *
        from fuel_machine_type
        where chat_id = :chatId
          and is_active = true
        order by sort_order, name
    """)
    List<FuelMachineType> findActive(long chatId);

    @Query("""
        select *
        from fuel_machine_type
        where chat_id = :chatId
          and name_norm = :nameNorm
        limit 1
    """)
    Optional<FuelMachineType> findByNorm(long chatId, String nameNorm);

    @Query("""
        select *
        from fuel_machine_type
        where chat_id = :chatId
          and name_norm like :pattern
        order by sort_order, name
        limit 20
    """)
    List<FuelMachineType> search(long chatId, String pattern);
}