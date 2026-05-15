package com.njxs.lims.audit;

import com.njxs.lims.auth.domain.ClientType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuditLogRepository implements AuditLogRepository {

  private static final String SELECT_COLUMNS = """
      id, occurred_at, actor_user_id, actor_role_code, client_type, action_code, entity_type,
      entity_id, entity_biz_no, before_snapshot::text as before_snapshot_json,
      after_snapshot::text as after_snapshot_json, result, failure_reason, ip, user_agent,
      trace_id
      """;

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

  @Override
  public List<AuditLogRecord> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from audit_log
        order by occurred_at desc, id desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject("select count(*) from audit_log", Integer.class);
    return count == null ? 0 : count;
  }

  private AuditLogRecord record(ResultSet rs) throws SQLException {
    return new AuditLogRecord(
        rs.getObject("id", UUID.class),
        rs.getTimestamp("occurred_at").toInstant(),
        rs.getObject("actor_user_id", UUID.class),
        rs.getString("actor_role_code"),
        ClientType.valueOf(rs.getString("client_type")),
        rs.getString("action_code"),
        rs.getString("entity_type"),
        rs.getObject("entity_id", UUID.class),
        rs.getString("entity_biz_no"),
        rs.getString("before_snapshot_json"),
        rs.getString("after_snapshot_json"),
        rs.getString("result"),
        rs.getString("failure_reason"),
        rs.getString("ip"),
        rs.getString("user_agent"),
        rs.getString("trace_id")
    );
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
