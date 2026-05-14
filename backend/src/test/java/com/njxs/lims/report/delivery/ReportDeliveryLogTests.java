package com.njxs.lims.report.delivery;

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
class ReportDeliveryLogTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID PORTAL_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

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
  ReportDeliveryLogTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void officialPdfDownloadWritesDeliveryLogWithGateStatus() throws Exception {
    String portalToken = login("portal", "portal.admin", "Portal123!");
    String financeToken = login("internal", "internal.finance", "Sales123!");
    String qualityToken = login("internal", "internal.admin", "Internal123!");
    UUID projectId = insertProject();
    UUID docxFileId = insertFile(
        "todo72-official.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    UUID reportId = insertIssuedReport(projectId, docxFileId);

    mockMvc.perform(post("/payment-receipts")
            .header(HttpHeaders.AUTHORIZATION, bearer(financeToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "amount": 900.00,
                  "receivedAt": "2026-05-14T20:00:00Z",
                  "method": "BANK_TRANSFER"
                }
                """.formatted(projectId)))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/reports/{id}/official-pdf", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(portalToken)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/report-delivery-logs")
            .queryParam("reportId", reportId.toString())
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].reportId").value(reportId.toString()))
        .andExpect(jsonPath("$.items[0].deliveryType").value("DOWNLOAD"))
        .andExpect(jsonPath("$.items[0].deliveredBy").value(PORTAL_ADMIN_ID.toString()))
        .andExpect(jsonPath("$.items[0].clientType").value("PORTAL"))
        .andExpect(jsonPath("$.items[0].paymentGateStatus").value("UNLOCKED"))
        .andExpect(jsonPath("$.items[0].fileId").isString());

    assertDeliveryLog(reportId);
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
          '00000000-0000-0000-0000-000000000301', 'STD_ACCREDITED', 'CMA', 'NORMAL',
          false, false, false, false, '00000000-0000-0000-0000-000000000104',
          '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000106',
          'PAYMENT_HOLD', 'NORMAL', 100, 'UNPAID', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        projectId,
        "P-TODO72-" + projectId,
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
        "reports/todo72/" + fileId + "/" + fileName,
        contentType,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private UUID insertIssuedReport(UUID projectId, UUID docxFileId) {
    UUID reportId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version,
          status, current_version_no, is_official, watermark_type, payment_gate_status,
          issued_at, created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, null, 'STERILITY_REPORT', '1.0',
          'ISSUED_PAYMENT_LOCKED', 1, true, 'NONE', 'LOCKED',
          now(), now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        reportId,
        "RPT-TODO72-" + reportId,
        projectId,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into report_version (
          id, report_id, version_no, status, docx_file_id, render_log, issued_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 1, 'ISSUED', ?, 'SIGNED',
          now(), now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        reportId,
        docxFileId,
        ADMIN_ID,
        ADMIN_ID
    );
    return reportId;
  }

  private void assertDeliveryLog(UUID reportId) {
    Map<String, Object> log = jdbcTemplate.queryForMap(
        """
        select dl.delivery_type, dl.delivered_by, dl.client_type, dl.payment_gate_status,
               dl.file_id, rv.pdf_file_id
        from report_delivery_log dl
        join report_version rv on rv.id = dl.report_version_id
        where dl.report_id = ?
        """,
        reportId
    );
    assertThat(log)
        .containsEntry("delivery_type", "DOWNLOAD")
        .containsEntry("delivered_by", PORTAL_ADMIN_ID)
        .containsEntry("client_type", "PORTAL")
        .containsEntry("payment_gate_status", "UNLOCKED");
    assertThat(log.get("file_id")).isNotNull().isEqualTo(log.get("pdf_file_id"));
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
