package com.njxs.lims.importexport.persistence;

import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcImportExportTaskRepository implements ImportExportTaskRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, task_type, entity_type, status, requested_by, requested_at,
      source_file_id, result_file_id, error_summary, expires_at
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcImportExportTaskRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "IET-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from import_export_task where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public ImportExportTaskRecord save(ImportExportTaskRecord record, String parametersJson, UUID actorUserId) {
    jdbcTemplate.queryForObject(
        "insert into import_export_task " + """
        (
          id, biz_no, task_type, entity_type, status, requested_by, requested_at,
          source_file_id, result_file_id, parameters, error_summary, expires_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        record.id(),
        record.bizNo(),
        record.taskType(),
        record.entityType(),
        record.status(),
        record.requestedBy(),
        Timestamp.from(record.requestedAt()),
        record.sourceFileId(),
        record.resultFileId(),
        parametersJson,
        record.errorSummary(),
        timestamp(record.expiresAt()),
        actorUserId,
        actorUserId
    );
    return record;
  }

  private ImportExportTaskRecord record(ResultSet rs) throws SQLException {
    return new ImportExportTaskRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getString("task_type"),
        rs.getString("entity_type"),
        rs.getString("status"),
        rs.getObject("requested_by", UUID.class),
        instant(rs.getTimestamp("requested_at")),
        rs.getObject("source_file_id", UUID.class),
        rs.getObject("result_file_id", UUID.class),
        rs.getString("error_summary"),
        instant(rs.getTimestamp("expires_at"))
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
