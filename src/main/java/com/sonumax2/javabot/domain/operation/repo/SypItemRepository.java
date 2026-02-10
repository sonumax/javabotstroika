package com.sonumax2.javabot.domain.operation.repo;

import com.sonumax2.javabot.domain.operation.SypItem;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.math.BigDecimal;
import java.util.List;

public interface SypItemRepository extends ListCrudRepository<SypItem, Long> {

    record Row(Long nomenclatureId, BigDecimal volume) {}

    @Query("""
        select nomenclature_id as nomenclatureId, volume as volume
        from syp_item
        where operation_id = :operationId
        order by id asc
    """)
    List<Row> listByOperationId(long operationId);

    @Modifying
    @Query("""
        delete from syp_item
        where operation_id = :operationId
    """)
    void deleteByOperationId(long operationId);

    @Modifying
    @Query("""
        insert into syp_item(operation_id, nomenclature_id, volume)
        values (:operationId, :nomenclatureId, :volume)
    """)
    void insertOne(long operationId, long nomenclatureId, BigDecimal volume);
}
