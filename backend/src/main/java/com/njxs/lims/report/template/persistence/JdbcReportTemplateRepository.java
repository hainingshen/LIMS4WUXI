package com.njxs.lims.report.template.persistence;

import com.njxs.lims.report.template.domain.ReportTemplateRecord;
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
public class JdbcReportTemplateRepository implements ReportTemplateRepository {

  private static final String SELECT_COLUMNS = """
      id, template_code, template_version, template_name, template_type, automation_level,
      file_id, status, published_at, published_by, legacy_template_name
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcReportTemplateRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public ReportTemplateRecord save(ReportTemplateRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into report_template (
          id, template_code, template_version, template_name, template_type, automation_level,
          file_id, status, published_at, published_by, legacy_template_name,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        record.id(),
        record.templateCode(),
        record.templateVersion(),
        record.templateName(),
        record.templateType(),
        record.automationLevel(),
        record.fileId(),
        record.status(),
        timestamp(record.publishedAt()),
        record.publishedBy(),
        record.legacyTemplateName(),
        actorUserId,
        actorUserId
    );
    return record;
  }

  @Override
  public Optional<ReportTemplateRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from report_template
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<ReportTemplateRecord> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from report_template
        where deleted_at is null
        order by template_code, template_version desc, created_at desc
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
        "select count(*) from report_template where deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public boolean existsByCodeAndVersion(String templateCode, String templateVersion) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report_template
        where template_code = ?
          and template_version = ?
          and deleted_at is null
        """,
        Integer.class,
        templateCode,
        templateVersion
    );
    return count != null && count > 0;
  }

  @Override
  public boolean fileExists(UUID fileId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from file_metadata where id = ? and deleted_at is null",
        Integer.class,
        fileId
    );
    return count != null && count > 0;
  }

  @Override
  public void deprecatePublishedByCode(String templateCode, UUID exceptId, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update report_template
        set status = 'DEPRECATED',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where template_code = ?
          and id <> ?
          and status = 'PUBLISHED'
          and deleted_at is null
        """,
        actorUserId,
        templateCode,
        exceptId
    );
  }

  @Override
  public ReportTemplateRecord publish(UUID id, Instant publishedAt, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update report_template " + """
        set status = 'PUBLISHED',
            published_at = ?,
            published_by = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        timestamp(publishedAt),
        actorUserId,
        actorUserId,
        id
    );
  }

  @Override
  public ReportTemplateRecord deprecate(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update report_template " + """
        set status = 'DEPRECATED',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        actorUserId,
        id
    );
  }

  private ReportTemplateRecord record(ResultSet rs) throws SQLException {
    return new ReportTemplateRecord(
        rs.getObject("id", UUID.class),
        rs.getString("template_code"),
        rs.getString("template_version"),
        rs.getString("template_name"),
        rs.getString("template_type"),
        rs.getString("automation_level"),
        rs.getObject("file_id", UUID.class),
        rs.getString("status"),
        instant(rs.getTimestamp("published_at")),
        rs.getObject("published_by", UUID.class),
        rs.getString("legacy_template_name")
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
