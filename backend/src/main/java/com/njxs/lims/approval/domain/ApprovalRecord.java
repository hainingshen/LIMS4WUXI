package com.njxs.lims.approval.domain;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRecord(
    UUID id,
    String entityType,
    UUID entityId,
    String entityBizNo,
    String stage,
    String outcome,
    String comment,
    UUID approverUserId,
    String approverRoleCode,
    Instant approvedAt,
    String traceId
) {
}
