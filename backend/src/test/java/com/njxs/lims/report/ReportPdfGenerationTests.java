package com.njxs.lims.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.auth.domain.ClientType;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.file.domain.StoredFile;
import com.njxs.lims.report.service.ReportPdfService;
import com.njxs.lims.report.service.ReportService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ReportPdfGenerationTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID SIGNATORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000112");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final ReportPdfService reportPdfService;
  private final ReportService reportService;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  ReportPdfGenerationTests(
      ReportPdfService reportPdfService,
      ReportService reportService,
      JdbcTemplate jdbcTemplate
  ) {
    this.reportPdfService = reportPdfService;
    this.reportService = reportService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void officialPdfRequiresIssuedReportAndStoresConvertedPdfFile() {
    UUID projectId = insertProject();
    UUID docxFileId = insertFile(
        "todo64-draft.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    UUID reportId = insertReportWithVersion(projectId, docxFileId);
    AuthPrincipal signatory = signatoryPrincipal();

    assertThatThrownBy(() -> reportPdfService.generateOfficialPdf(reportId, signatory))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getCode()).isEqualTo("REPORT_NOT_ISSUED"));

    jdbcTemplate.update("update report set status = 'PENDING_ISSUE' where id = ?", reportId);
    reportService.issue(reportId, signatory);

    StoredFile pdf = reportPdfService.generateOfficialPdf(reportId, signatory);
    assertThat(pdf.fileId()).isNotNull();
    assertThat(pdf.contentType()).isEqualTo("application/pdf");
    assertConvertedPdf(reportId, pdf.fileId());
  }

  private UUID insertProject() {
    UUID projectId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into project (
          id, biz_no, submission_id, customer_org_id, project_type, qualification_type, service_type,
          requires_glp, requires_animal_test, requires_ethics_review, requires_nonstandard_review,
          business_owner_id, technical_owner_id, sd_user_id, status, health_status, health_score,
          payment_status, created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, 'P-TODO64', '00000000-0000-0000-0000-000000000401',
          '00000000-0000-0000-0000-000000000301', 'STD_ACCREDITED', 'CMA', 'NORMAL',
          false, false, false, false, '00000000-0000-0000-0000-000000000104',
          '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000106',
          'REPORTING', 'NORMAL', 100, 'UNPAID', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        projectId,
        ADMIN_ID,
        ADMIN_ID
    );
    return projectId;
  }

  private UUID insertFile(String fileName, String contentType) {
    UUID fileId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into file_metadata (
          id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, 1024, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        fileId,
        fileName,
        "reports/todo64/" + fileId + "/" + fileName,
        contentType,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private UUID insertReportWithVersion(UUID projectId, UUID docxFileId) {
    UUID reportId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version,
          status, current_version_no, is_official, watermark_type, payment_gate_status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, 'RPT-TODO64', ?, null, 'STERILITY_REPORT', '1.0',
          'DRAFT', 1, false, 'DRAFT', 'LOCKED',
          now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        reportId,
        projectId,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into report_version (
          id, report_id, version_no, status, docx_file_id, render_log,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 1, 'DRAFT', ?, 'TEST_DRAFT',
          now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        reportId,
        docxFileId,
        ADMIN_ID,
        ADMIN_ID
    );
    return reportId;
  }

  private void assertConvertedPdf(UUID reportId, UUID pdfFileId) {
    Map<String, Object> version = jdbcTemplate.queryForMap(
        """
        select rv.status, rv.pdf_file_id, f.content_type
        from report_version rv
        join file_metadata f on f.id = rv.pdf_file_id
        where rv.report_id = ?
          and rv.version_no = 1
        """,
        reportId
    );
    assertThat(version)
        .containsEntry("status", "ISSUED")
        .containsEntry("pdf_file_id", pdfFileId)
        .containsEntry("content_type", "application/pdf");
  }

  private AuthPrincipal signatoryPrincipal() {
    return new AuthPrincipal(
        SIGNATORY_ID,
        ClientType.INTERNAL,
        List.of("AUTHORIZED_SIGNATORY"),
        null,
        "todo64"
    );
  }
}
