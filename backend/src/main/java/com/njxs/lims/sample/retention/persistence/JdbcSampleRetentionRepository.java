package com.njxs.lims.sample.retention.persistence;

import com.njxs.lims.sample.retention.domain.SampleRetentionRecord;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSampleRetentionRepository implements SampleRetentionRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, sample_id, project_id, report_id, retained_quantity, quantity_unit,
      storage_condition, storage_location, retained_at, retained_by, retain_until,
      disposition_plan, status, proof_file_id
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcSampleRetentionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "SR-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from sample_retention where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public SampleRetentionRecord save(SampleRetentionRecord record) {
    jdbcTemplate.update(
        """
        insert into sample_retention (
          id, biz_no, sample_id, project_id, report_id, retained_quantity, quantity_unit,
          storage_condition, storage_location, retained_at, retained_by, retain_until,
          disposition_plan, status, proof_file_id, created_at, created_by, updated_at,
          updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        record.id(),
        record.bizNo(),
        record.sampleId(),
        record.projectId(),
        record.reportId(),
        record.retainedQuantity(),
        record.quantityUnit(),
        record.storageCondition(),
        record.storageLocation(),
        timestamp(record.retainedAt()),
        record.retainedBy(),
        date(record.retainUntil()),
        record.dispositionPlan(),
        record.status(),
        record.proofFileId(),
        timestamp(record.retainedAt()),
        record.retainedBy(),
        timestamp(record.retainedAt()),
        record.retainedBy(),
        0,
        "NEW",
        100
    );
    return record;
  }

  @Override
  public Optional<SampleRetentionRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + " from sample_retention where id = ? and deleted_at is null",
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<SampleRetentionRecord> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from sample_retention
        where deleted_at is null
        order by retain_until, biz_no
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from sample_retention where deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public SampleRetentionRecord updateStatus(UUID id, String status, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update sample_retention
        set status = ?, updated_at = now(), updated_by = ?, version = version + 1
        where id = ? and deleted_at is null
        """,
        status,
        actorUserId,
        id
    );
    return findById(id).orElseThrow();
  }

  @Override
  public SampleRetentionRecord extend(UUID id, LocalDate retainUntil, String reason, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update sample_retention
        set retain_until = ?,
            disposition_plan = 'EXTEND',
            status = 'RETAINED',
            extension_reason = ?,
            extended_at = now(),
            extended_by = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        """,
        date(retainUntil),
        reason,
        actorUserId,
        actorUserId,
        id
    );
    return findById(id).orElseThrow();
  }

  @Override
  public SampleRetentionRecord dispose(
      UUID id,
      String disposition,
      String status,
      String dispositionReason,
      UUID proofFileId,
      UUID actorUserId
  ) {
    jdbcTemplate.update(
        """
        update sample_retention
        set status = ?,
            disposition_plan = ?,
            proof_file_id = ?,
            disposition_reason = ?,
            disposed_at = now(),
            disposed_by = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        """,
        status,
        disposition,
        proofFileId,
        dispositionReason,
        actorUserId,
        actorUserId,
        id
    );
    return findById(id).orElseThrow();
  }

  @Override
  public boolean paymentUnlocked(UUID id) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from sample_retention sr
        join project p on p.id = sr.project_id
        where sr.id = ?
          and sr.deleted_at is null
          and p.deleted_at is null
          and p.payment_status in ('PAID', 'GREEN_CHANNEL')
        """,
        Integer.class,
        id
    );
    return count != null && count > 0;
  }

  private SampleRetentionRecord record(ResultSet rs) throws SQLException {
    return new SampleRetentionRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("sample_id", UUID.class),
        rs.getObject("project_id", UUID.class),
        rs.getObject("report_id", UUID.class),
        rs.getBigDecimal("retained_quantity"),
        rs.getString("quantity_unit"),
        rs.getString("storage_condition"),
        rs.getString("storage_location"),
        instant(rs.getTimestamp("retained_at")),
        rs.getObject("retained_by", UUID.class),
        localDate(rs.getDate("retain_until")),
        rs.getString("disposition_plan"),
        rs.getString("status"),
        rs.getObject("proof_file_id", UUID.class)
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private Date date(LocalDate date) {
    return date == null ? null : Date.valueOf(date);
  }

  private LocalDate localDate(Date date) {
    return date == null ? null : date.toLocalDate();
  }
}
