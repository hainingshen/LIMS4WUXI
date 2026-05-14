package com.njxs.lims.report.delivery.persistence;

import com.njxs.lims.report.delivery.domain.ReportDeliveryLogRecord;
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
public class JdbcReportDeliveryLogRepository implements ReportDeliveryLogRepository {

  private static final String SELECT_COLUMNS = """
      id, report_id, report_version_id, delivery_type, delivered_to, delivered_by,
      delivered_at, client_type, payment_gate_status, file_id
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcReportDeliveryLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ReportDeliveryLogRecord save(ReportDeliveryLogRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into report_delivery_log (
          id, report_id, report_version_id, delivery_type, delivered_to, delivered_by,
          delivered_at, client_type, payment_gate_status, file_id, created_at, created_by,
          updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        record.id(),
        record.reportId(),
        record.reportVersionId(),
        record.deliveryType(),
        record.deliveredTo(),
        record.deliveredBy(),
        Timestamp.from(record.deliveredAt()),
        record.clientType(),
        record.paymentGateStatus(),
        record.fileId(),
        actorUserId,
        actorUserId
    );
    return record;
  }

  @Override
  public Optional<UUID> currentVersionId(UUID reportId) {
    return jdbcTemplate.query(
        """
        select rv.id
        from report r
        join report_version rv on rv.report_id = r.id
          and rv.version_no = r.current_version_no
          and rv.deleted_at is null
        where r.id = ?
          and r.deleted_at is null
        """,
        (rs, rowNum) -> rs.getObject("id", UUID.class),
        reportId
    ).stream().findFirst();
  }

  @Override
  public List<ReportDeliveryLogRecord> list(UUID reportId, int limit, int offset) {
    if (reportId == null) {
      return jdbcTemplate.query(
          "select " + SELECT_COLUMNS + """
          from report_delivery_log
          where deleted_at is null
          order by delivered_at desc, id desc
          limit ? offset ?
          """,
          (rs, rowNum) -> record(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from report_delivery_log
        where report_id = ?
          and deleted_at is null
        order by delivered_at desc, id desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        reportId,
        limit,
        offset
    );
  }

  @Override
  public int count(UUID reportId) {
    Integer count;
    if (reportId == null) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from report_delivery_log where deleted_at is null",
          Integer.class
      );
    } else {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from report_delivery_log
          where report_id = ?
            and deleted_at is null
          """,
          Integer.class,
          reportId
      );
    }
    return count == null ? 0 : count;
  }

  private ReportDeliveryLogRecord record(ResultSet rs) throws SQLException {
    return new ReportDeliveryLogRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("report_id", UUID.class),
        rs.getObject("report_version_id", UUID.class),
        rs.getString("delivery_type"),
        rs.getString("delivered_to"),
        rs.getObject("delivered_by", UUID.class),
        instant(rs.getTimestamp("delivered_at")),
        rs.getString("client_type"),
        rs.getString("payment_gate_status"),
        rs.getObject("file_id", UUID.class)
    );
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
