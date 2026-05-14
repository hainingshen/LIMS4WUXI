package com.njxs.lims.audit;

import java.sql.Timestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuditLogRepository implements AuditLogRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcAuditLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void append(AuditLogEntry entry) {
    jdbcTemplate.update(
        """
        insert into audit_log (
          id,
          occurred_at,
          actor_user_id,
          actor_role_code,
          client_type,
          action_code,
          entity_type,
          entity_id,
          entity_biz_no,
          result,
          failure_reason,
          ip,
          user_agent,
          trace_id
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        entry.id(),
        Timestamp.from(entry.occurredAt()),
        entry.actorUserId(),
        entry.actorRoleCode(),
        entry.clientType().name(),
        entry.actionCode(),
        blankToNull(entry.entityType()),
        entry.entityId(),
        blankToNull(entry.entityBizNo()),
        entry.result(),
        blankToNull(entry.failureReason()),
        blankToNull(entry.ip()),
        blankToNull(entry.userAgent()),
        blankToNull(entry.traceId())
    );
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
