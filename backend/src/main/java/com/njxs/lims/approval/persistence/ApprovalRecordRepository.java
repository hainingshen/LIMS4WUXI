package com.njxs.lims.approval.persistence;

import com.njxs.lims.approval.domain.ApprovalRecord;
import java.util.List;
import java.util.UUID;

public interface ApprovalRecordRepository {

  ApprovalRecord save(ApprovalRecord record);

  List<ApprovalRecord> findByEntity(String entityType, UUID entityId);

  boolean existsByEntityStageAndOutcome(String entityType, UUID entityId, String stage, String outcome);
}
