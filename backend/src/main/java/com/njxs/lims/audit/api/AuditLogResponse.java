package com.njxs.lims.audit.api;

import com.njxs.lims.audit.AuditLogRecord;
import com.njxs.lims.auth.domain.ClientType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    Instant occurredAt,
    UUID actorUserId,
    String actorRoleCode,
    ClientType clientType,
    String actionCode,
    String entityType,
    UUID entityId,
    String entityBizNo,
    Map<String, Object> beforeSnapshot,
    Map<String, Object> afterSnapshot,
    String result,
    String failureReason,
    String ip,
    String userAgent,
    String traceId
) {
  public static AuditLogResponse from(
      AuditLogRecord record,
      Map<String, Object> beforeSnapshot,
      Map<String, Object> afterSnapshot
  ) {
    return new AuditLogResponse(
        record.id(),
        record.occurredAt(),
        record.actorUserId(),
        record.actorRoleCode(),
        record.clientType(),
        record.actionCode(),
        record.entityType(),
        record.entityId(),
        record.entityBizNo(),
        beforeSnapshot,
        afterSnapshot,
        record.result(),
        record.failureReason(),
        record.ip(),
        record.userAgent(),
        record.traceId()
    );
  }
}
