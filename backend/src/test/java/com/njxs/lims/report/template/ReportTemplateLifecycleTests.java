package com.njxs.lims.report.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ReportTemplateLifecycleTests {

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
  ReportTemplateLifecycleTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void publishingNewVersionDeprecatesCurrentVersionWithoutChangingHistoricalReports() throws Exception {
    String adminToken = login("internal", "internal.admin", "Internal123!");
    UUID fileV1 = insertFile("sterility-report-v1.docx");
    UUID fileV2 = insertFile("sterility-report-v2.docx");

    UUID templateV1 = createTemplate(adminToken, "1.0", fileV1);
    mockMvc.perform(post("/report-templates")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(templatePayload("1.0", insertFile("duplicate.docx"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REPORT_TEMPLATE_VERSION_EXISTS"));

    publish(templateV1, adminToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"))
        .andExpect(jsonPath("$.publishedBy").value(ADMIN_ID.toString()));
    assertPublishedCount(1);

    UUID reportId = insertHistoricalReport(templateV1);
    UUID templateV2 = createTemplate(adminToken, "2.0", fileV2);
    publish(templateV2, adminToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.templateVersion").value("2.0"))
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    assertPublishedCount(1);
    assertTemplateStatus(templateV1, "DEPRECATED");
    assertTemplateStatus(templateV2, "PUBLISHED");
    assertReportTemplatePointer(reportId, templateV1);

    mockMvc.perform(get("/report-templates")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));

    mockMvc.perform(post("/report-templates/{id}/deprecate", templateV2)
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DEPRECATED"));
    assertPublishedCount(0);
  }

  private org.springframework.test.web.servlet.ResultActions publish(UUID id, String adminToken) throws Exception {
    return mockMvc.perform(post("/report-templates/{id}/publish", id)
        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)));
  }

  private UUID createTemplate(String adminToken, String version, UUID fileId) throws Exception {
    String response = mockMvc.perform(post("/report-templates")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(templatePayload(version, fileId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.templateCode").value("STERILITY_REPORT"))
        .andExpect(jsonPath("$.templateVersion").value(version))
        .andExpect(jsonPath("$.templateType").value("REPORT"))
        .andExpect(jsonPath("$.automationLevel").value("A"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private String templatePayload(String version, UUID fileId) {
    return """
        {
          "templateCode": "STERILITY_REPORT",
          "templateVersion": "%s",
          "templateName": "Sterility Report %s",
          "templateType": "REPORT",
          "automationLevel": "A",
          "fileId": "%s",
          "legacyTemplateName": "legacy-sterility-%s"
        }
        """.formatted(version, version, fileId, version);
  }

  private UUID insertFile(String fileName) {
    UUID fileId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into file_metadata (
          id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
          1024, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        fileId,
        fileName,
        "templates/" + fileId + "/" + fileName,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private UUID insertHistoricalReport(UUID templateId) {
    UUID projectId = insertProject();
    UUID reportId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version,
          status, current_version_no, is_official, watermark_type, payment_gate_status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, 'STERILITY_REPORT', '1.0', 'DRAFT', 1, false, 'DRAFT', 'LOCKED',
          now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        reportId,
        "RPT-TODO60-" + UUID.randomUUID(),
        projectId,
        templateId,
        ADMIN_ID,
        ADMIN_ID
    );
    return reportId;
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
          'REPORTING', 'NORMAL', 100, 'UNPAID', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        projectId,
        "P-TODO60-" + UUID.randomUUID(),
        ADMIN_ID,
        ADMIN_ID
    );
    return projectId;
  }

  private void assertPublishedCount(int expected) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report_template
        where template_code = 'STERILITY_REPORT'
          and status = 'PUBLISHED'
          and deleted_at is null
        """,
        Integer.class
    );
    assertThat(count).isEqualTo(expected);
  }

  private void assertTemplateStatus(UUID templateId, String expectedStatus) {
    String status = jdbcTemplate.queryForObject(
        "select status from report_template where id = ?",
        String.class,
        templateId
    );
    assertThat(status).isEqualTo(expectedStatus);
  }

  private void assertReportTemplatePointer(UUID reportId, UUID expectedTemplateId) {
    UUID actualTemplateId = jdbcTemplate.queryForObject(
        "select report_template_id from report where id = ?",
        UUID.class,
        reportId
    );
    assertThat(actualTemplateId).isEqualTo(expectedTemplateId);
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
