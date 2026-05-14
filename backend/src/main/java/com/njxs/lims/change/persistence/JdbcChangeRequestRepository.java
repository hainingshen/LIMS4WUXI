package com.njxs.lims.change.persistence;

import com.njxs.lims.change.domain.ChangeRequestItemRecord;
import com.njxs.lims.change.domain.ChangeRequestRecord;
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
public class JdbcChangeRequestRepository implements ChangeRequestRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcChangeRequestRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "CR-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from change_request where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public ChangeRequestRecord save(ChangeRequestRecord record) {
    jdbcTemplate.update(
        """
        insert into change_request (
          id, biz_no, target_type, target_id, target_biz_no, change_type, reason, status,
          requested_by, requested_at, effective_at, attachment_file_id, created_at, created_by,
          updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.id(),
        record.bizNo(),
        record.targetType(),
        record.targetId(),
        record.targetBizNo(),
        record.changeType(),
        record.reason(),
        record.status(),
        record.requestedBy(),
        timestamp(record.requestedAt()),
        timestamp(record.effectiveAt()),
        record.attachmentFileId(),
        timestamp(record.requestedAt()),
        record.requestedBy(),
        timestamp(record.requestedAt()),
        record.requestedBy(),
        0,
        "NEW",
        100
    );
    for (int index = 0; index < record.items().size(); index++) {
      ChangeRequestItemRecord item = record.items().get(index);
      jdbcTemplate.update(
          """
          insert into change_request_item (
            id, change_request_id, field_path, field_label, old_value, new_value, lock_policy, sort_order
          ) values (?, ?, ?, ?, ?, ?, ?, ?)
          """,
          UUID.randomUUID(),
          record.id(),
          item.fieldPath(),
          item.fieldLabel(),
          item.oldValue(),
          item.newValue(),
          item.lockPolicy(),
          index
      );
    }
    return record;
  }

  @Override
  public List<ChangeRequestRecord> list(int limit, int offset) {
    List<ChangeRequestRecord> records = jdbcTemplate.query(
        """
        select id, biz_no, target_type, target_id, target_biz_no, change_type, reason, status,
               requested_by, requested_at, effective_at, attachment_file_id
        from change_request
        where deleted_at is null
        order by requested_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs, List.of()),
        limit,
        offset
    );
    return records.stream()
        .map(record -> new ChangeRequestRecord(
            record.id(),
            record.bizNo(),
            record.targetType(),
            record.targetId(),
            record.targetBizNo(),
            record.changeType(),
            record.reason(),
            record.status(),
            record.requestedBy(),
            record.requestedAt(),
            record.effectiveAt(),
            record.attachmentFileId(),
            items(record.id())
        ))
        .toList();
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from change_request where deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public boolean issuedReportExistsForSample(UUID sampleId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report r
        join sample s on s.project_id = r.project_id
        where s.id = ?
          and r.deleted_at is null
          and r.status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED', 'RELEASED', 'ARCHIVED')
        """,
        Integer.class,
        sampleId
    );
    return count != null && count > 0;
  }

  private List<ChangeRequestItemRecord> items(UUID changeRequestId) {
    return jdbcTemplate.query(
        """
        select field_path, field_label, old_value, new_value, lock_policy
        from change_request_item
        where change_request_id = ?
        order by sort_order
        """,
        (rs, rowNum) -> new ChangeRequestItemRecord(
            rs.getString("field_path"),
            rs.getString("field_label"),
            rs.getString("old_value"),
            rs.getString("new_value"),
            rs.getString("lock_policy")
        ),
        changeRequestId
    );
  }

  private ChangeRequestRecord record(ResultSet rs, List<ChangeRequestItemRecord> items) throws SQLException {
    return new ChangeRequestRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getString("target_type"),
        rs.getObject("target_id", UUID.class),
        rs.getString("target_biz_no"),
        rs.getString("change_type"),
        rs.getString("reason"),
        rs.getString("status"),
        rs.getObject("requested_by", UUID.class),
        instant(rs.getTimestamp("requested_at")),
        instant(rs.getTimestamp("effective_at")),
        rs.getObject("attachment_file_id", UUID.class),
        items
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
