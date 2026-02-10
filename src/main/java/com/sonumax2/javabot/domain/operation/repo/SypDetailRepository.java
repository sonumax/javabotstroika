package com.sonumax2.javabot.domain.operation.repo;

import com.sonumax2.javabot.domain.operation.SypDetail;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface SypDetailRepository extends CrudRepository<SypDetail, Long> {

    @Modifying
    @Query("""
    insert into syp_detail(operation_id, work_object_id, counterparty_id, pay_type, receipt_type)
    values (:operationId, :workObjectId, :counterpartyId, :payType, :docType)
  """)
    void insertOne(long operationId, long workObjectId, long counterpartyId, String payType, String docType);
}