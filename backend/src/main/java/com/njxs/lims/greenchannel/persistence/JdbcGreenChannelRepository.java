package com.njxs.lims.greenchannel.persistence;

import com.njxs.lims.greenchannel.domain.GreenChannelRequestRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcGreenChannelRepository implements GreenChannelRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, report_id, reason, status, requested_by, requested_at, unlock_scope
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcGreenChannelRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "GC-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from green_channel_request where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public GreenChannelRequestRecord save(GreenChannelRequestRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into green_channel_request (
          id, biz_no, project_id, report_id, reason, status, requested_by, requested_at,
          unlock_scope, created_at, created_by, updated_at, updated_by, version,
          source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.reportId(),
        record.reason(),
        record.status(),
        record.requestedBy(),
        Timestamp.from(record.requestedAt()),
        record.unlockScope(),
        actorUserId,
        actorUserId
    );
    return record;
  }

  @Override
  public Optional<GreenChannelRequestRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from green_channel_request
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<GreenChannelRequestRecord> list(UUID projectId, int limit, int offset) {
    if (projectId == null) {
      return jdbcTemplate.query(
          "select " + SELECT_COLUMNS + """
          from green_channel_request
          where deleted_at is null
          order by requested_at desc, biz_no desc
          limit ? offset ?
          """,
          (rs, rowNum) -> record(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from green_channel_request
        where project_id = ?
          and deleted_at is null
        order by requested_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        projectId,
        limit,
        offset
    );
  }

  @Override
  public int count(UUID projectId) {
    Integer count;
    if (projectId == null) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from green_channel_request where deleted_at is null",
          Integer.class
      );
    } else {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from green_channel_request
          where project_id = ?
            and deleted_at is null
          """,
          Integer.class,
          projectId
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public boolean reportBelongsToProject(UUID reportId, UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report
        where id = ?
          and project_id = ?
          and deleted_at is null
        """,
        Integer.class,
        reportId,
        projectId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean reportAssignedSales(UUID reportId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report r
        join project p on p.id = r.project_id
        join submission s on s.id = p.submission_id
        where r.id = ?
          and s.assigned_sales_user_id = ?
          and r.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        """,
        Integer.class,
        reportId,
        userId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsById(UUID id) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from green_channel_request where id = ? and deleted_at is null",
        Integer.class,
        id
    );
    return count != null && count > 0;
  }

  @Override
  public GreenChannelRequestRecord updateStatus(UUID id, String status, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update green_channel_request " + """
        set status = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        status,
        actorUserId,
        id
    );
  }

  @Override
  public void greenUnlockReport(UUID reportId, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update report
        set payment_gate_status = 'GREEN_UNLOCKED',
            status = case when status = 'ISSUED_PAYMENT_LOCKED' then 'GREEN_RELEASED' else status end,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED', 'RELEASED')
          and deleted_at is null
        """,
        actorUserId,
        reportId
    );
  }

  private GreenChannelRequestRecord record(ResultSet rs) throws SQLException {
    return new GreenChannelRequestRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("project_id", UUID.class),
        rs.getObject("report_id", UUID.class),
        rs.getString("reason"),
        rs.getString("status"),
        rs.getObject("requested_by", UUID.class),
        instant(rs.getTimestamp("requested_at")),
        rs.getString("unlock_scope")
    );
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
