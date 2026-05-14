package com.njxs.lims.report;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ReportReviewFlowTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

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
  ReportReviewFlowTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void reportReviewFlowRejectsToDraftAndIssuesPaymentLockedReport() throws Exception {
    String preparerToken = login("internal", "internal.report_preparer", "Sales123!");
    String qcToken = login("internal", "internal.qc", "Sales123!");
    String qaToken = login("internal", "internal.qa", "Sales123!");
    String sdToken = login("internal", "internal.sd", "Sales123!");
    String signatoryToken = login("internal", "internal.authorized_signatory", "Sales123!");
    UUID projectId = insertProject();
    UUID reportId = insertDraftReport(projectId);

    submitQc(reportId, preparerToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QC_REVIEWING"));
    review(reportId, qcToken, "QC", "REJECTED", "QC rework")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"));
    assertApproval(reportId, "QC", "REJECTED");

    submitQc(reportId, preparerToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QC_REVIEWING"));
    review(reportId, qcToken, "QC", "APPROVED", "QC pass")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QA_REVIEWING"));
    review(reportId, qaToken, "QA", "APPROVED", "QA pass")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SD_REVIEWING"));
    review(reportId, sdToken, "SD", "APPROVED", "SD pass")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_ISSUE"));

    mockMvc.perform(post("/reports/{id}/issue", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(signatoryToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ISSUED_PAYMENT_LOCKED"))
        .andExpect(jsonPath("$.watermarkType").value("NONE"))
        .andExpect(jsonPath("$.paymentGateStatus").value("LOCKED"));

    assertApproval(reportId, "QC", "APPROVED");
    assertApproval(reportId, "QA", "APPROVED");
    assertApproval(reportId, "SD", "APPROVED");
    assertApproval(reportId, "ISSUE", "APPROVED");
    assertIssued(reportId);
  }

  private org.springframework.test.web.servlet.ResultActions submitQc(UUID reportId, String token) throws Exception {
    return mockMvc.perform(post("/reports/{id}/submit-qc", reportId)
        .header(HttpHeaders.AUTHORIZATION, bearer(token)));
  }

  private org.springframework.test.web.servlet.ResultActions review(
      UUID reportId,
      String token,
      String stage,
      String outcome,
      String comment
  ) throws Exception {
    return mockMvc.perform(post("/reports/{reportId}/review", reportId)
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"stage":"%s","outcome":"%s","comment":"%s"}
            """.formatted(stage, outcome, comment)));
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
        ) values (?, 'P-TODO63', '00000000-0000-0000-0000-000000000401',
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

  private UUID insertDraftReport(UUID projectId) {
    UUID reportId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version,
          status, current_version_no, is_official, watermark_type, payment_gate_status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, 'RPT-TODO63', ?, null, 'STERILITY_REPORT', '1.0',
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
          id, report_id, version_no, status, render_log, created_at, created_by,
          updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 1, 'DRAFT', 'TEST_DRAFT', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        reportId,
        ADMIN_ID,
        ADMIN_ID
    );
    return reportId;
  }

  private void assertApproval(UUID reportId, String stage, String outcome) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from approval_record
        where entity_type = 'report'
          and entity_id = ?
          and stage = ?
          and outcome = ?
        """,
        Integer.class,
        reportId,
        stage,
        outcome
    );
    assertThat(count).isEqualTo(1);
  }

  private void assertIssued(UUID reportId) {
    Map<String, Object> report = jdbcTemplate.queryForMap(
        """
        select r.status, r.is_official, r.watermark_type, r.payment_gate_status, rv.status version_status
        from report r
        join report_version rv on rv.report_id = r.id and rv.version_no = r.current_version_no
        where r.id = ?
        """,
        reportId
    );
    assertThat(report)
        .containsEntry("status", "ISSUED_PAYMENT_LOCKED")
        .containsEntry("is_official", true)
        .containsEntry("watermark_type", "NONE")
        .containsEntry("payment_gate_status", "LOCKED")
        .containsEntry("version_status", "ISSUED");
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
