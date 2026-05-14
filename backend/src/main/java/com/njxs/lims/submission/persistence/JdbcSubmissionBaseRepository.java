package com.njxs.lims.submission.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.submission.domain.Submission;
import com.njxs.lims.submission.domain.SubmissionTestSelection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSubmissionBaseRepository implements SubmissionBaseRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSubmissionBaseRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void saveSubmission(Submission submission) {
    prepareForInsert(submission);
    jdbcTemplate.update(
        """
        insert into submission (
          id, biz_no, customer_org_id, customer_contact_id, manufacturer_id, applicant_user_id,
          assigned_sales_user_id, status, official_report_email, shipping_info, requires_glp,
          requires_hard_copy_of_electronic_report, report_language, protocol_language,
          submitted_quantity_text, sample_disposition, sample_disposition_remark, change_locked_at,
          glp_characterization_affirmed, glp_characterization_statement_version,
          glp_characterization_confirmed_by, glp_characterization_confirmed_at, created_at,
          created_by, updated_at, updated_by, deleted_at, version, tenant_org_id, legacy_id,
          source_type, migration_batch, data_quality_score
        ) values (
          ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        )
        """,
        submission.getId(),
        submission.getBizNo(),
        submission.getCustomerOrgId(),
        submission.getCustomerContactId(),
        submission.getManufacturerId(),
        submission.getApplicantUserId(),
        submission.getAssignedSalesUserId(),
        submission.getStatus(),
        submission.getOfficialReportEmail(),
        submission.getShippingInfo(),
        submission.isRequiresGlp(),
        submission.isRequiresHardCopyOfElectronicReport(),
        submission.getReportLanguage(),
        submission.getProtocolLanguage(),
        submission.getSubmittedQuantityText(),
        submission.getSampleDisposition(),
        submission.getSampleDispositionRemark(),
        timestamp(submission.getChangeLockedAt()),
        submission.getGlpCharacterizationAffirmed(),
        submission.getGlpCharacterizationStatementVersion(),
        submission.getGlpCharacterizationConfirmedBy(),
        timestamp(submission.getGlpCharacterizationConfirmedAt()),
        timestamp(submission.getCreatedAt()),
        submission.getCreatedBy(),
        timestamp(submission.getUpdatedAt()),
        submission.getUpdatedBy(),
        timestamp(submission.getDeletedAt()),
        submission.getVersion(),
        submission.getTenantOrgId(),
        submission.getLegacyId(),
        submission.getSourceType(),
        submission.getMigrationBatch(),
        submission.getDataQualityScore()
    );
  }

  @Override
  public void saveTestSelection(SubmissionTestSelection selection) {
    prepareForInsert(selection);
    jdbcTemplate.update(
        """
        insert into submission_test_selection (
          id, biz_no, submission_id, category, test_code, test_name, method_name, extractant,
          standard, instruction, note, created_at, created_by, updated_at, updated_by, deleted_at,
          version, tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        selection.getId(),
        selection.getBizNo(),
        selection.getSubmissionId(),
        selection.getCategory(),
        selection.getTestCode(),
        selection.getTestName(),
        selection.getMethodName(),
        selection.getExtractant(),
        selection.getStandard(),
        selection.getInstruction(),
        selection.getNote(),
        timestamp(selection.getCreatedAt()),
        selection.getCreatedBy(),
        timestamp(selection.getUpdatedAt()),
        selection.getUpdatedBy(),
        timestamp(selection.getDeletedAt()),
        selection.getVersion(),
        selection.getTenantOrgId(),
        selection.getLegacyId(),
        selection.getSourceType(),
        selection.getMigrationBatch(),
        selection.getDataQualityScore()
    );
  }
}
