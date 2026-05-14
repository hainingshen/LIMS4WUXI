package com.njxs.lims.approval.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.approval.domain.ApprovalRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcApprovalRecordRepository implements ApprovalRecordRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcApprovalRecordRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ApprovalRecord save(ApprovalRecord record) {
    jdbcTemplate.update(
        """
        insert into approval_record (
          id, entity_type, entity_id, entity_biz_no, stage, outcome, comment,
          approver_user_id, approver_role_code, approved_at, trace_id
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.id(),
        record.entityType(),
        record.entityId(),
        record.entityBizNo(),
        record.stage(),
        record.outcome(),
        record.comment(),
        record.approverUserId(),
        record.approverRoleCode(),
        timestamp(record.approvedAt()),
        record.traceId()
    );
    return record;
  }

  @Override
  public List<ApprovalRecord> findByEntity(String entityType, UUID entityId) {
    return jdbcTemplate.query(
        """
        select id, entity_type, entity_id, entity_biz_no, stage, outcome, comment,
          approver_user_id, approver_role_code, approved_at, trace_id
        from approval_record
        where entity_type = ? and entity_id = ?
        order by approved_at, stage, id
        """,
        (rs, rowNum) -> record(rs),
        entityType,
        entityId
    );
  }

  @Override
  public boolean existsByEntityStageAndOutcome(String entityType, UUID entityId, String stage, String outcome) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from approval_record
        where entity_type = ? and entity_id = ? and stage = ? and outcome = ?
        """,
        Integer.class,
        entityType,
        entityId,
        stage,
        outcome
    );
    return count != null && count > 0;
  }

  private ApprovalRecord record(ResultSet rs) throws SQLException {
    return new ApprovalRecord(
        rs.getObject("id", UUID.class),
        rs.getString("entity_type"),
        rs.getObject("entity_id", UUID.class),
        rs.getString("entity_biz_no"),
        rs.getString("stage"),
        rs.getString("outcome"),
        rs.getString("comment"),
        rs.getObject("approver_user_id", UUID.class),
        rs.getString("approver_role_code"),
        instant(rs.getTimestamp("approved_at")),
        rs.getString("trace_id")
    );
  }

  private Instant instant(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant();
  }
}
