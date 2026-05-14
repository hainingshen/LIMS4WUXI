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
class ReportGenerationTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID INSPECTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000107");
  private static final UUID QC_ID = UUID.fromString("00000000-0000-0000-0000-000000000109");

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
  ReportGenerationTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void draftGenerationRequiresCompletedWorkOrdersAndCreatesReportVersion() throws Exception {
    String reportPreparerToken = login("internal", "internal.report_preparer", "Sales123!");
    UUID templateId = insertPublishedTemplate();
    UUID projectId = insertProject();
    UUID sampleId = insertSample(projectId);
    UUID planId = insertStudyPlan(projectId);
    UUID workOrderId = insertWorkOrder(planId, projectId, sampleId, "RESULT_ENTERED");
    insertRawRecord(workOrderId, projectId, sampleId);

    mockMvc.perform(post("/reports/generate-draft")
            .header(HttpHeaders.AUTHORIZATION, bearer(reportPreparerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(generateDraftPayload(projectId, workOrderId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REPORT_WORK_ORDER_NOT_COMPLETED"));

    jdbcTemplate.update("update work_order set status = 'COMPLETED', progress_percent = 100 where id = ?", workOrderId);

    String response = mockMvc.perform(post("/reports/generate-draft")
            .header(HttpHeaders.AUTHORIZATION, bearer(reportPreparerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(generateDraftPayload(projectId, workOrderId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId.toString()))
        .andExpect(jsonPath("$.reportTemplateId").value(templateId.toString()))
        .andExpect(jsonPath("$.templateCode").value("STERILITY_REPORT"))
        .andExpect(jsonPath("$.templateVersion").value("1.0"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.watermarkType").value("DRAFT"))
        .andExpect(jsonPath("$.paymentGateStatus").value("LOCKED"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID reportId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

    assertReportVersion(reportId);
    assertReportWorkOrder(reportId, workOrderId);
  }

  private String generateDraftPayload(UUID projectId, UUID workOrderId) {
    return """
        {
          "projectId": "%s",
          "templateCode": "STERILITY_REPORT",
          "workOrderIds": ["%s"]
        }
        """.formatted(projectId, workOrderId);
  }

  private UUID insertPublishedTemplate() {
    UUID templateId = UUID.randomUUID();
    UUID fileId = insertFile("sterility-report-template.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    jdbcTemplate.update(
        """
        insert into report_template (
          id, template_code, template_version, template_name, template_type, automation_level,
          file_id, status, published_at, published_by, created_at, created_by, updated_at,
          updated_by, version, source_type, data_quality_score
        ) values (?, 'STERILITY_REPORT', '1.0', 'Sterility Report', 'REPORT', 'A',
          ?, 'PUBLISHED', now(), ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        templateId,
        fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return templateId;
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
        "reports/test/" + fileId + "/" + fileName,
        contentType,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
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
        ) values (?, 'P-TODO62', '00000000-0000-0000-0000-000000000401',
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

  private UUID insertSample(UUID projectId) {
    UUID sampleId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into sample (
          id, biz_no, project_id, submission_id, sample_name_zh, sample_name_en,
          sterilization_state, cas_code, model_ref, size_spec, lot_batch, raw_material,
          packaging_material, physical_state, color, density, stability, solubility,
          storage_condition, intended_use, additional_info, quantity, quantity_unit,
          status, received_at, received_date, created_at, created_by, updated_at,
          updated_by, version, source_type, data_quality_score
        ) values (?, 'SAMPLE-TODO62', ?, '00000000-0000-0000-0000-000000000401',
          'Todo 62 Sample', 'Todo 62 Sample', 'NON_STERILE', 'N/A', 'MODEL-62',
          '10 mm', 'LOT-62', 'Polymer', 'Pouch', 'SOLID', 'White', 'N/A',
          'Stable', 'Insoluble', 'ROOM_TEMPERATURE', 'Validation sample',
          'No additional info', 10, 'pcs', 'TEST_COMPLETED',
          '2026-05-14T00:00:00Z', '2026-05-14', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        sampleId,
        projectId,
        ADMIN_ID,
        ADMIN_ID
    );
    return sampleId;
  }

  private UUID insertStudyPlan(UUID projectId) {
    UUID planId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into study_plan (
          id, biz_no, project_id, status, requires_conditioning, requires_glp,
          requires_animal_test, sd_approved_at, created_at, updated_at, version,
          source_type, data_quality_score
        ) values (?, 'PLAN-TODO62', ?, 'COMPLETED', false, false, false,
          now(), now(), now(), 0, 'NEW', 100)
        """,
        planId,
        projectId
    );
    return planId;
  }

  private UUID insertWorkOrder(UUID planId, UUID projectId, UUID sampleId, String status) {
    UUID workOrderId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into work_order (
          id, biz_no, study_plan_id, project_id, sample_id, owner_user_id, status,
          progress_percent, last_progress_at, created_at, updated_at, version,
          source_type, data_quality_score
        ) values (?, 'WO-TODO62', ?, ?, ?, ?, ?, 80,
          now(), now(), now(), 0, 'NEW', 100)
        """,
        workOrderId,
        planId,
        projectId,
        sampleId,
        INSPECTOR_ID,
        status
    );
    return workOrderId;
  }

  private void insertRawRecord(UUID workOrderId, UUID projectId, UUID sampleId) {
    jdbcTemplate.update(
        """
        insert into raw_record (
          id, biz_no, work_order_id, project_id, sample_id, template_code, test_item,
          test_basis, test_time, test_location, temperature_celsius, humidity_percent,
          method, record_data, attachment_file_ids, status, tester_id, checker_id,
          submitted_at, reviewed_at, created_at, created_by, updated_at, updated_by,
          version, source_type, data_quality_score
        ) values (?, 'RR-TODO62', ?, ?, ?, 'RAW_RECORD_STERILITY', 'STERILITY',
          'ISO 11737', '2026-05-14 - 2026-05-21', 'Lab A', '22', '45',
          'DIRECT_INOCULATION', ?::jsonb, '[]'::jsonb, 'APPROVED', ?, ?,
          now(), now(), now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        workOrderId,
        projectId,
        sampleId,
        rawRecordData(),
        INSPECTOR_ID,
        QC_ID,
        INSPECTOR_ID,
        QC_ID
    );
  }

  private String rawRecordData() {
    return """
        {
          "equipmentUsages": [
            {"name": "Incubator", "no": "EQ-1"}
          ],
          "sterilityResults": [
            {"sampleName": "Todo 62 Sample", "conclusion": "无菌"}
          ]
        }
        """;
  }

  private void assertReportVersion(UUID reportId) {
    Map<String, Object> version = jdbcTemplate.queryForMap(
        """
        select rv.status, rv.docx_file_id, rv.data_snapshot_file_id,
               docx.content_type docx_content_type,
               snapshot.content_type snapshot_content_type
        from report_version rv
        join file_metadata docx on docx.id = rv.docx_file_id
        join file_metadata snapshot on snapshot.id = rv.data_snapshot_file_id
        where rv.report_id = ?
        """,
        reportId
    );
    assertThat(version)
        .containsEntry("status", "DRAFT")
        .containsEntry("docx_content_type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        .containsEntry("snapshot_content_type", "application/json");
    assertThat(version.get("docx_file_id")).isNotNull();
    assertThat(version.get("data_snapshot_file_id")).isNotNull();
  }

  private void assertReportWorkOrder(UUID reportId, UUID workOrderId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report_work_order
        where report_id = ? and work_order_id = ? and raw_record_id is not null
        """,
        Integer.class,
        reportId,
        workOrderId
    );
    assertThat(count).isEqualTo(1);
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
