package com.sonumax2.javabot.domain.operation.repo;

import com.sonumax2.javabot.domain.operation.FuelDetail;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface FuelDetailRepository extends CrudRepository<FuelDetail, Long> {

    @Modifying
    @Query("""
        insert into fuel_detail(
            operation_id,
            object_id,
            counterparty_id,
            machine_type_id,
            fuel_kind,
            volume,
            amount,
            date,
            note,
            doc_type,
            doc_file_id
        )
        values (
            :operationId,
            :objectId,
            :counterpartyId,
            :machineTypeId,
            :fuelKind,
            :volume,
            :amount,
            :date,
            :note,
            :docType,
            :docFileId
        )
    """)
    void insertOne(
            long operationId,
            long objectId,
            long counterpartyId,
            Long machineTypeId,      // nullable (для TRANSPORT)
            String fuelKind,         // enum -> name()
            java.math.BigDecimal volume,
            java.math.BigDecimal amount,
            java.time.LocalDate date,
            String note,             // nullable
            String docType,          // enum -> name()
            String docFileId         // nullable
    );
}
