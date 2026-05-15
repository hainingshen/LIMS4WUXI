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
  private static final String SELECT_REPORT_COLUMNS = "r."
      + SELECT_COLUMNS.strip().replaceAll("\\s+", " ").replace(", ", ", r.");

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
  public boolean existsById(UUID id) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from report where id = ? and deleted_at is null",
        Integer.class,
        id
    );
    return count != null && count > 0;
  }

  @Override
  public boolean reportCustomerOrgMatches(UUID reportId, UUID customerOrgId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report r
        join project p on p.id = r.project_id
        where r.id = ?
          and p.customer_org_id = ?
          and r.deleted_at is null
          and p.deleted_at is null
        """,
        Integer.class,
        reportId,
        customerOrgId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean reportApplicantMatches(UUID reportId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report r
        join project p on p.id = r.project_id
        join submission s on s.id = p.submission_id
        join app_user u on u.id = s.applicant_user_id
        where r.id = ?
          and s.applicant_user_id = ?
          and p.customer_org_id = u.customer_org_id
          and s.customer_org_id = u.customer_org_id
          and u.client_type = 'PORTAL'
          and u.customer_org_id is not null
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
  public List<ReportRecord> listForCustomerOrg(UUID customerOrgId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_REPORT_COLUMNS + " " + """
        from report r
        join project p on p.id = r.project_id
        where p.customer_org_id = ?
          and r.deleted_at is null
          and p.deleted_at is null
        order by r.created_at desc, r.biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> report(rs),
        customerOrgId,
        limit,
        offset
    );
  }

  @Override
  public int countForCustomerOrg(UUID customerOrgId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report r
        join project p on p.id = r.project_id
        where p.customer_org_id = ?
          and r.deleted_at is null
          and p.deleted_at is null
        """,
        Integer.class,
        customerOrgId
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<ReportRecord> listForApplicant(UUID applicantUserId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_REPORT_COLUMNS + " " + """
        from report r
        join project p on p.id = r.project_id
        join submission s on s.id = p.submission_id
        join app_user u on u.id = s.applicant_user_id
        where s.applicant_user_id = ?
          and p.customer_org_id = u.customer_org_id
          and s.customer_org_id = u.customer_org_id
          and u.client_type = 'PORTAL'
          and u.customer_org_id is not null
          and r.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        order by r.created_at desc, r.biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> report(rs),
        applicantUserId,
        limit,
        offset
    );
  }

  @Override
  public int countForApplicant(UUID applicantUserId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report r
        join project p on p.id = r.project_id
        join submission s on s.id = p.submission_id
        join app_user u on u.id = s.applicant_user_id
        where s.applicant_user_id = ?
          and p.customer_org_id = u.customer_org_id
          and s.customer_org_id = u.customer_org_id
          and u.client_type = 'PORTAL'
          and u.customer_org_id is not null
          and r.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        """,
        Integer.class,
        applicantUserId
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
          id, report_id, version_no, status, docx_file_id, pdf_file_id, data_snapshot_file_id,
          render_log, created_at, created_by, updated_at, updated_by, version,
          source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        version.id(),
        version.reportId(),
        version.versionNo(),
        version.status(),
        version.docxFileId(),
        version.pdfFileId(),
        version.dataSnapshotFileId(),
        version.renderLog(),
        actorUserId,
        actorUserId
    );
    return version;
  }

  @Override
  public Optional<ReportVersionRecord> findVersion(UUID reportId, int versionNo) {
    return jdbcTemplate.query(
        """
        select id, report_id, version_no, status, docx_file_id, pdf_file_id,
               data_snapshot_file_id, render_log
        from report_version
        where report_id = ?
          and version_no = ?
          and deleted_at is null
        """,
        (rs, rowNum) -> version(rs),
        reportId,
        versionNo
    ).stream().findFirst();
  }

  @Override
  public ReportRecord updateStatus(UUID id, String status, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update report " + """
        set status = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> report(rs),
        status,
        actorUserId,
        id
    );
  }

  @Override
  public ReportRecord issue(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update report " + """
        set status = 'ISSUED_PAYMENT_LOCKED',
            is_official = true,
            watermark_type = 'NONE',
            payment_gate_status = 'LOCKED',
            issued_at = now(),
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> report(rs),
        actorUserId,
        id
    );
  }

  @Override
  public void updateCurrentVersionStatus(UUID reportId, int versionNo, String status, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update report_version
        set status = ?,
            issued_at = case when ? = 'ISSUED' then now() else issued_at end,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where report_id = ?
          and version_no = ?
          and deleted_at is null
        """,
        status,
        status,
        actorUserId,
        reportId,
        versionNo
    );
  }

  @Override
  public void updateCurrentVersionPdfFile(UUID reportId, int versionNo, UUID pdfFileId, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update report_version
        set pdf_file_id = ?,
            render_log = concat_ws(';', nullif(render_log, ''), 'PDF_CONVERTED'),
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where report_id = ?
          and version_no = ?
          and deleted_at is null
        """,
        pdfFileId,
        actorUserId,
        reportId,
        versionNo
    );
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

  private ReportVersionRecord version(ResultSet rs) throws SQLException {
    return new ReportVersionRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("report_id", UUID.class),
        rs.getInt("version_no"),
        rs.getString("status"),
        rs.getObject("docx_file_id", UUID.class),
        rs.getObject("pdf_file_id", UUID.class),
        rs.getObject("data_snapshot_file_id", UUID.class),
        rs.getString("render_log")
    );
  }

  private String placeholders(int count) {
    return "?,".repeat(count - 1) + "?";
  }
}
