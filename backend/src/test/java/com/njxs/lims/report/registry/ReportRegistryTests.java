package com.njxs.lims.report.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ReportRegistryTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID CUSTOMER_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  ReportRegistryTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void searchesCurrentReportWithHistoricalVersionsAndCreatesBatchDownloadTask() throws Exception {
    String qualityToken = login("internal", "internal.admin", "Internal123!");
    UUID templateId = insertReportTemplate();
    UUID projectId = insertProject();
    UUID reportId = insertReleasedReportWithHistory(projectId, templateId);

    mockMvc.perform(get("/report-registry/search")
            .queryParam("projectId", projectId.toString())
            .queryParam("customerOrgId", CUSTOMER_ORG_ID.toString())
            .queryParam("reportBizNo", reportBizNo(reportId))
            .queryParam("templateType", "REPORT")
            .queryParam("status", "RELEASED")
            .queryParam("issuedFrom", "2026-05-14T00:00:00Z")
            .queryParam("issuedTo", "2026-05-15T00:00:00Z")
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].reportId").value(reportId.toString()))
        .andExpect(jsonPath("$.items[0].projectId").value(projectId.toString()))
        .andExpect(jsonPath("$.items[0].currentVersionNo").value(2))
        .andExpect(jsonPath("$.items[0].status").value("RELEASED"))
        .andExpect(jsonPath("$.items[0].paymentGateStatus").value("UNLOCKED"))
        .andExpect(jsonPath("$.items[0].deliveryCount").value(1))
        .andExpect(jsonPath("$.items[0].versions.length()").value(2))
        .andExpect(jsonPath("$.items[0].versions[0].versionNo").value(2))
        .andExpect(jsonPath("$.items[0].versions[1].status").value("SUPERSEDED"));

    MvcResult created = mockMvc.perform(post("/report-registry/batch-download")
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "reportIds": ["%s"],
                  "includeDeliveryLog": true
                }
                """.formatted(reportId)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.taskType").value("REPORT_BATCH_DOWNLOAD"))
        .andExpect(jsonPath("$.entityType").value("REPORT"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();

    UUID taskId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
        .get("id")
        .asText());
    assertBatchDownloadTask(taskId, reportId);
  }

  private UUID insertReportTemplate() {
    UUID templateId = UUID.randomUUID();
    UUID fileId = insertFile("todo75-template.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    jdbcTemplate.update(
        """
        insert into report_template (
          id, template_code, template_version, template_name, template_type,
          automation_level, file_id, status, published_at, published_by,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, '1.0', 'TODO75 Registry Template', 'REPORT',
          'A', ?, 'PUBLISHED', now(), ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        templateId,
        "TODO75_" + templateId.toString().substring(0, 8),
        fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return templateId;
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
        ) values (?, ?, '00000000-0000-0000-0000-000000000401',
          ?, 'STD_ACCREDITED', 'CMA', 'NORMAL',
          false, false, false, false, '00000000-0000-0000-0000-000000000104',
          '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000106',
          'RELEASED', 'NORMAL', 100, 'PAID', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        projectId,
        "P-TODO75-" + projectId,
        CUSTOMER_ORG_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return projectId;
  }

  private UUID insertReleasedReportWithHistory(UUID projectId, UUID templateId) {
    UUID reportId = UUID.randomUUID();
    UUID versionOneId = UUID.randomUUID();
    UUID versionTwoId = UUID.randomUUID();
    UUID docxFileId = insertFile("todo75-current.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    UUID pdfFileId = insertFile("todo75-current.pdf", "application/pdf");
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version,
          status, current_version_no, is_official, watermark_type, payment_gate_status,
          issued_at, released_at, created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, 'STERILITY_REPORT', '1.0',
          'RELEASED', 2, true, 'NONE', 'UNLOCKED',
          '2026-05-14T12:00:00Z', '2026-05-14T13:00:00Z', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        reportId,
        reportBizNo(reportId),
        projectId,
        templateId,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into report_version (
          id, report_id, version_no, status, docx_file_id, render_log, issued_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 1, 'SUPERSEDED', ?, 'SUPERSEDED',
          '2026-05-14T10:00:00Z', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        versionOneId,
        reportId,
        docxFileId,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into report_version (
          id, report_id, version_no, status, docx_file_id, pdf_file_id, render_log, issued_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 2, 'ISSUED', ?, ?, 'SIGNED',
          '2026-05-14T12:00:00Z', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        versionTwoId,
        reportId,
        docxFileId,
        pdfFileId,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into report_delivery_log (
          id, report_id, report_version_id, delivery_type, delivered_by, delivered_at,
          client_type, payment_gate_status, file_id, created_at, created_by,
          updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, 'INTERNAL_DOWNLOAD', ?, now(),
          'INTERNAL', 'UNLOCKED', ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        reportId,
        versionTwoId,
        ADMIN_ID,
        pdfFileId,
        ADMIN_ID,
        ADMIN_ID
    );
    return reportId;
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
        "reports/todo75/" + fileId + "/" + fileName,
        contentType,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private void assertBatchDownloadTask(UUID taskId, UUID reportId) {
    Map<String, Object> task = jdbcTemplate.queryForMap(
        """
        select task_type, entity_type, status, requested_by, parameters::text parameters
        from import_export_task
        where id = ?
        """,
        taskId
    );
    assertThat(task)
        .containsEntry("task_type", "REPORT_BATCH_DOWNLOAD")
        .containsEntry("entity_type", "REPORT")
        .containsEntry("status", "PENDING")
        .containsEntry("requested_by", ADMIN_ID);
    assertThat((String) task.get("parameters")).contains(reportId.toString());
  }

  private String reportBizNo(UUID reportId) {
    return "RPT-TODO75-" + reportId;
  }

  private String login(String channel, String username, String password) throws Exception {
    String response = mockMvc.perform(post("/auth/{channel}/login", channel)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode token = objectMapper.readTree(response);
    return token.get("accessToken").asText();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
