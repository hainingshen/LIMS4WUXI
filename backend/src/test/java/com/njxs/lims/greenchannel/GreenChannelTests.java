package com.njxs.lims.greenchannel;

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
class GreenChannelTests {

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
  GreenChannelTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void twoLevelApprovalGreenUnlocksReportWithoutMarkingProjectPaid() throws Exception {
    String salesToken = login("internal", "internal.sales", "Sales123!");
    String deptToken = login("internal", "internal.dept_manager", "Sales123!");
    String generalToken = login("internal", "internal.general_manager", "Sales123!");
    String portalToken = login("portal", "portal.admin", "Portal123!");
    UUID projectId = insertProject();
    UUID docxFileId = insertFile(
        "todo71-green-channel.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    UUID reportId = insertIssuedLockedReport(projectId, docxFileId);

    MvcResult created = mockMvc.perform(post("/green-channel-requests")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "reportId": "%s",
                  "reason": "Customer needs the electronic report before payment settlement."
                }
                """.formatted(projectId, reportId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId.toString()))
        .andExpect(jsonPath("$.reportId").value(reportId.toString()))
        .andExpect(jsonPath("$.status").value("DEPT_REVIEWING"))
        .andExpect(jsonPath("$.unlockScope").value("ELECTRONIC_REPORT_ONLY"))
        .andReturn();
    UUID requestId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
        .get("id")
        .asText());

    mockMvc.perform(post("/green-channel-requests/{id}/approve", requestId)
            .header(HttpHeaders.AUTHORIZATION, bearer(deptToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stage": "DEPT",
                  "outcome": "APPROVED",
                  "comment": "Department approved."
                }
                """))
        .andExpect(status().isNoContent());
    assertRequestStatus(requestId, "MANAGEMENT_REVIEWING");

    mockMvc.perform(post("/green-channel-requests/{id}/approve", requestId)
            .header(HttpHeaders.AUTHORIZATION, bearer(generalToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stage": "MANAGEMENT",
                  "outcome": "APPROVED",
                  "comment": "Management approved."
                }
                """))
        .andExpect(status().isNoContent());

    assertGreenUnlocked(requestId, reportId, projectId);
    MvcResult download = mockMvc.perform(get("/reports/{id}/official-pdf", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(portalToken)))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(download.getResponse().getContentType()).isEqualTo("application/pdf");
    assertThat(download.getResponse().getContentAsByteArray()).isNotEmpty();
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
        "P-TODO71-" + projectId,
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
        "reports/todo71/" + fileId + "/" + fileName,
        contentType,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private UUID insertIssuedLockedReport(UUID projectId, UUID docxFileId) {
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
        "RPT-TODO71-" + reportId,
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

  private void assertRequestStatus(UUID requestId, String expectedStatus) {
    String status = jdbcTemplate.queryForObject(
        "select status from green_channel_request where id = ?",
        String.class,
        requestId
    );
    assertThat(status).isEqualTo(expectedStatus);
  }

  private void assertGreenUnlocked(UUID requestId, UUID reportId, UUID projectId) {
    Map<String, Object> state = jdbcTemplate.queryForMap(
        """
        select p.payment_status, r.status as report_status, r.payment_gate_status, g.status as green_status
        from project p
        join report r on r.project_id = p.id
        join green_channel_request g on g.report_id = r.id
        where p.id = ?
          and r.id = ?
          and g.id = ?
        """,
        projectId,
        reportId,
        requestId
    );
    assertThat(state)
        .containsEntry("payment_status", "UNPAID")
        .containsEntry("report_status", "GREEN_RELEASED")
        .containsEntry("payment_gate_status", "GREEN_UNLOCKED")
        .containsEntry("green_status", "APPROVED");

    Integer approvals = jdbcTemplate.queryForObject(
        """
        select count(*)
        from approval_record
        where entity_type = 'green_channel_request'
          and entity_id = ?
          and stage in ('DEPT', 'MANAGEMENT')
          and outcome = 'APPROVED'
        """,
        Integer.class,
        requestId
    );
    assertThat(approvals).isEqualTo(2);

    Integer greenChannelLogs = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report_delivery_log
        where report_id = ?
          and delivery_type = 'GREEN_CHANNEL'
          and payment_gate_status = 'GREEN_UNLOCKED'
        """,
        Integer.class,
        reportId
    );
    assertThat(greenChannelLogs).isEqualTo(1);
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
