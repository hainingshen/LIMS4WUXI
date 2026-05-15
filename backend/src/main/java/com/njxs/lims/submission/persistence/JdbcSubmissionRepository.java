package com.njxs.lims.submission.persistence;

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
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSubmissionRepository implements SubmissionRepository {

  private static final String SELECT_COLUMNS = """
      id,
      biz_no,
      customer_org_id,
      customer_contact_id,
      manufacturer_id,
      applicant_user_id,
      assigned_sales_user_id,
      form_code,
      schema_version,
      data_json,
      status,
      official_report_email,
      shipping_info,
      requires_glp,
      requires_hard_copy_of_electronic_report,
      report_language,
      protocol_language,
      submitted_quantity_text,
      sample_disposition,
      sample_disposition_remark,
      change_locked_at
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcSubmissionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<SubmissionRecord> findById(UUID id) {
    List<SubmissionRecord> records = jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from submission
        where id = ?
          and deleted_at is null
        """,
        (rs, rowNum) -> record(rs),
        id
    );
    return records.stream().findFirst();
  }

  @Override
  public String nextSubmissionBizNo(int year) {
    String prefix = "SUB-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from submission where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    int sequence = (count == null ? 0 : count) + 1;
    return prefix + "%04d".formatted(sequence);
  }

  @Override
  public Optional<UUID> findDefaultSalesUserId() {
    List<UUID> salesUsers = jdbcTemplate.queryForList(
        """
        select user_id
        from user_role
        where role_code = 'SALES_REP'
        order by user_id
        limit 1
        """,
        UUID.class
    );
    return salesUsers.stream().findFirst();
  }

  @Override
  public List<SubmissionRecord> listForApplicant(UUID applicantUserId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from submission
        where applicant_user_id = ?
          and deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        applicantUserId,
        limit,
        offset
    );
  }

  @Override
  public int countForApplicant(UUID applicantUserId) {
    return count(
        """
        select count(*)
        from submission
        where applicant_user_id = ?
          and deleted_at is null
        """,
        applicantUserId
    );
  }

  @Override
  public List<SubmissionRecord> listForCustomerOrg(UUID customerOrgId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from submission
        where customer_org_id = ?
          and deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        customerOrgId,
        limit,
        offset
    );
  }

  @Override
  public int countForCustomerOrg(UUID customerOrgId) {
    return count(
        """
        select count(*)
        from submission
        where customer_org_id = ?
          and deleted_at is null
        """,
        customerOrgId
    );
  }

  @Override
  public List<SubmissionRecord> listSubmittedForAssignedSales(UUID salesUserId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from submission
        where assigned_sales_user_id = ?
          and status in ('SUBMITTED', 'ACCEPTED')
          and deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        salesUserId,
        limit,
        offset
    );
  }

  @Override
  public int countSubmittedForAssignedSales(UUID salesUserId) {
    return count(
        """
        select count(*)
        from submission
        where assigned_sales_user_id = ?
          and status in ('SUBMITTED', 'ACCEPTED')
          and deleted_at is null
        """,
        salesUserId
    );
  }

  @Override
  public boolean existsByApplicant(UUID id, UUID applicantUserId) {
    return count(
        """
        select count(*)
        from submission
        where id = ?
          and applicant_user_id = ?
          and deleted_at is null
        """,
        id,
        applicantUserId
    ) > 0;
  }

  @Override
  public boolean existsByCustomerOrg(UUID id, UUID customerOrgId) {
    return count(
        """
        select count(*)
        from submission
        where id = ?
          and customer_org_id = ?
          and deleted_at is null
        """,
        id,
        customerOrgId
    ) > 0;
  }

  @Override
  public boolean existsByAssignedSales(UUID id, UUID salesUserId) {
    return count(
        """
        select count(*)
        from submission
        where id = ?
          and assigned_sales_user_id = ?
          and deleted_at is null
        """,
        id,
        salesUserId
    ) > 0;
  }

  @Override
  public SubmissionRecord markSubmitted(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update submission " + """
        set status = 'SUBMITTED',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        actorUserId,
        id
    );
  }

  @Override
  public SubmissionRecord markAccepted(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update submission " + """
        set status = 'ACCEPTED',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        actorUserId,
        id
    );
  }

  @Override
  public SubmissionRecord markProjectSplit(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update submission " + """
        set status = 'PROJECT_SPLIT',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        actorUserId,
        id
    );
  }

  @Override
  public SubmissionRecord updateDraftData(
      UUID id,
      String formCode,
      int schemaVersion,
      String dataJson,
      String officialReportEmail,
      String shippingInfo,
      boolean requiresGlp,
      boolean requiresHardCopyOfElectronicReport,
      String reportLanguage,
      String protocolLanguage,
      String submittedQuantityText,
      String sampleDisposition,
      String sampleDispositionRemark,
      UUID actorUserId
  ) {
    return jdbcTemplate.queryForObject(
        "update submission " + """
        set form_code = ?,
            schema_version = ?,
            data_json = ?::jsonb,
            official_report_email = ?,
            shipping_info = ?,
            requires_glp = ?,
            requires_hard_copy_of_electronic_report = ?,
            report_language = ?,
            protocol_language = ?,
            submitted_quantity_text = ?,
            sample_disposition = ?,
            sample_disposition_remark = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        formCode,
        schemaVersion,
        dataJson,
        officialReportEmail,
        shippingInfo,
        requiresGlp,
        requiresHardCopyOfElectronicReport,
        reportLanguage,
        protocolLanguage,
        submittedQuantityText,
        sampleDisposition,
        sampleDispositionRemark,
        actorUserId,
        id
    );
  }

  @Override
  public void markChangeLocked(UUID id, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update submission
        set change_locked_at = coalesce(change_locked_at, now()),
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        """,
        actorUserId,
        id
    );
  }

  private SubmissionRecord record(ResultSet rs) throws SQLException {
    return new SubmissionRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("customer_org_id", UUID.class),
        rs.getObject("customer_contact_id", UUID.class),
        rs.getObject("manufacturer_id", UUID.class),
        rs.getObject("applicant_user_id", UUID.class),
        rs.getObject("assigned_sales_user_id", UUID.class),
        rs.getString("form_code"),
        rs.getInt("schema_version"),
        rs.getString("data_json"),
        rs.getString("status"),
        rs.getString("official_report_email"),
        rs.getString("shipping_info"),
        rs.getBoolean("requires_glp"),
        rs.getBoolean("requires_hard_copy_of_electronic_report"),
        rs.getString("report_language"),
        rs.getString("protocol_language"),
        rs.getString("submitted_quantity_text"),
        rs.getString("sample_disposition"),
        rs.getString("sample_disposition_remark"),
        instant(rs.getTimestamp("change_locked_at"))
    );
  }

  private int count(String sql, Object... args) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  private Instant instant(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant();
  }
}
