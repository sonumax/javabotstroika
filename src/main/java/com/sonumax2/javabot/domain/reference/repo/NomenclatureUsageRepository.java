package com.sonumax2.javabot.domain.reference.repo;

import com.sonumax2.javabot.domain.reference.NomenclatureUsage;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface NomenclatureUsageRepository extends ListCrudRepository<NomenclatureUsage, Long> {

    @Query("""
        select *
        from nomenclature_usage
        where usage = :usage
        order by id asc
    """)
    List<NomenclatureUsage> findByUsage(String usage);

    @Modifying
    @Query("""
        delete from nomenclature_usage
        where nomenclature_id = :nomId and usage = :usage
    """)
    void deleteByNomIdAndUsage(Long nomId, String usage);

    @Query("""
        select count(*) > 0
        from nomenclature_usage
        where nomenclature_id = :nomId and usage = :usage
    """)
    boolean existsByNomIdAndUsage(Long nomId, String usage);

    @Modifying
    @Query("""
        insert into nomenclature_usage(nomenclature_id, usage)
        values (:nomenclatureId, :usage)
        on conflict (nomenclature_id, usage) do nothing
    """)
    void touch(long nomenclatureId, String usageType);
}
