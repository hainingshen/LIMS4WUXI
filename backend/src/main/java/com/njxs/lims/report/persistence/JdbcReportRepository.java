package com.njxs.lims.report.persistence;

import com.njxs.lims.report.domain.ReportRecord;
import com.njxs.lims.report.domain.ReportVersionRecord;
import com.njxs.lims.report.domain.ReportWorkOrderRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcReportRepository implements ReportRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, report_template_id, template_code, template_version, status,
      current_version_no, is_official, watermark_type, payment_gate_status
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcReportRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "RPT-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from report where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public ReportRecord save(ReportRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version,
          status, current_version_no, is_official, watermark_type, payment_gate_status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.reportTemplateId(),
        record.templateCode(),
        record.templateVersion(),
        record.status(),
        record.currentVersionNo(),
        record.official(),
        record.watermarkType(),
        record.paymentGateStatus(),
        actorUserId,
        actorUserId
    );
    return record;
  }

  @Override
  public Optional<ReportRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from report
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> report(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<ReportRecord> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from report
        where deleted_at is null
        order by created_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> report(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from report where deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<ReportWorkOrderRecord> findWorkOrders(UUID projectId, List<UUID> workOrderIds) {
    Object[] args = new Object[workOrderIds.size() + 1];
    args[0] = projectId;
    for (int index = 0; index < workOrderIds.size(); index++) {
      args[index + 1] = workOrderIds.get(index);
    }
    return jdbcTemplate.query(
        """
        select wo.id work_order_id,
               wo.status,
               rr.id raw_record_id
        from work_order wo
        left join lateral (
          select id
          from raw_record
          where work_order_id = wo.id
            and status = 'APPROVED'
            and deleted_at is null
          order by submitted_at desc
          limit 1
        ) rr on true
        where wo.project_id = ?
          and wo.id in (%s)
          and wo.deleted_at is null
        """.formatted(placeholders(workOrderIds.size())),
        (rs, rowNum) -> new ReportWorkOrderRecord(
            rs.getObject("work_order_id", UUID.class),
            rs.getObject("raw_record_id", UUID.class),
            rs.getString("status")
        ),
        args
    );
  }

  @Override
  public boolean workOrdersBelongToProject(UUID projectId, List<UUID> workOrderIds) {
    Object[] args = new Object[workOrderIds.size() + 1];
    args[0] = projectId;
    for (int index = 0; index < workOrderIds.size(); index++) {
      args[index + 1] = workOrderIds.get(index);
    }
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from work_order
        where project_id = ?
          and id in (%s)
          and deleted_at is null
        """.formatted(placeholders(workOrderIds.size())),
        Integer.class,
        args
    );
    return count != null && count == workOrderIds.size();
  }

  @Override
  public Map<UUID, UUID> findApprovedRawRecordIds(List<UUID> workOrderIds) {
    List<ReportWorkOrderRecord> records = jdbcTemplate.query(
        """
        select wo.id work_order_id,
               wo.status,
               rr.id raw_record_id
        from work_order wo
        left join lateral (
          select id
          from raw_record
          where work_order_id = wo.id
            and status = 'APPROVED'
            and deleted_at is null
          order by submitted_at desc
          limit 1
        ) rr on true
        where wo.id in (%s)
          and wo.deleted_at is null
        """.formatted(placeholders(workOrderIds.size())),
        (rs, rowNum) -> new ReportWorkOrderRecord(
            rs.getObject("work_order_id", UUID.class),
            rs.getObject("raw_record_id", UUID.class),
            rs.getString("status")
        ),
        workOrderIds.toArray()
    );
    Map<UUID, UUID> rawRecords = new LinkedHashMap<>();
    for (ReportWorkOrderRecord record : records) {
      rawRecords.put(record.workOrderId(), record.rawRecordId());
    }
    return rawRecords;
  }

  @Override
  public void linkWorkOrders(UUID reportId, Map<UUID, UUID> rawRecordIds) {
    rawRecordIds.forEach((workOrderId, rawRecordId) -> jdbcTemplate.update(
        """
        insert into report_work_order (report_id, work_order_id, raw_record_id)
        values (?, ?, ?)
        """,
        reportId,
        workOrderId,
        rawRecordId
    ));
  }

  @Override
  public UUID saveGeneratedFileMetadata(
      String fileName,
      String objectKey,
      String contentType,
      long sizeBytes,
      String sha256,
      UUID actorUserId
  ) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into file_metadata (
          id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        id,
        fileName,
        objectKey,
        contentType,
        sizeBytes,
        sha256,
        actorUserId,
        actorUserId,
        actorUserId
    );
    return id;
  }

  @Override
  public ReportVersionRecord saveVersion(ReportVersionRecord version, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into report_version (
          id, report_id, version_no, status, docx_file_id, data_snapshot_file_id,
          render_log, created_at, created_by, updated_at, updated_by, version,
          source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        version.id(),
        version.reportId(),
        version.versionNo(),
        version.status(),
        version.docxFileId(),
        version.dataSnapshotFileId(),
        version.renderLog(),
        actorUserId,
        actorUserId
    );
    return version;
  }

  private ReportRecord report(ResultSet rs) throws SQLException {
    return new ReportRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("project_id", UUID.class),
        rs.getObject("report_template_id", UUID.class),
        rs.getString("template_code"),
        rs.getString("template_version"),
        rs.getString("status"),
        rs.getInt("current_version_no"),
        rs.getBoolean("is_official"),
        rs.getString("watermark_type"),
        rs.getString("payment_gate_status")
    );
  }

  private String placeholders(int count) {
    return "?,".repeat(count - 1) + "?";
  }
}
