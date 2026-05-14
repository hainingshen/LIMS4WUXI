package com.njxs.lims.audit;

import com.njxs.lims.auth.domain.ClientType;
import java.time.Instant;
import java.util.UUID;

public record AuditLogEntry(
    UUID id,
    Instant occurredAt,
    UUID actorUserId,
    String actorRoleCode,
    ClientType clientType,
    String actionCode,
    String entityType,
    UUID entityId,
    String entityBizNo,
    String result,
    String failureReason,
    String ip,
    String userAgent,
    String traceId
) {
}
