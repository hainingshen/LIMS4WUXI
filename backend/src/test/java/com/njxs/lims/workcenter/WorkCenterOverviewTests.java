package com.njxs.lims.workcenter;

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
class WorkCenterOverviewTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID SALES_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
  private static final UUID TECH_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
  private static final UUID BUSINESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");
  private static final UUID SD_ID = UUID.fromString("00000000-0000-0000-0000-000000000106");
  private static final UUID INSPECTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000107");
  private static final UUID SALES_SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
  private static final UUID ADMIN_SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

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
  WorkCenterOverviewTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void qualityManagerSeesAllRiskProjectsAndSalesSeesAssignedScopeOnly() throws Exception {
    String qualityToken = login("internal", "internal.admin", "Internal123!");
    String salesToken = login("internal", "internal.sales", "Sales123!");
    jdbcTemplate.update("update submission set assigned_sales_user_id = ? where id = ?", ADMIN_ID, ADMIN_SUBMISSION_ID);
    UUID salesProjectId = insertProject(SALES_SUBMISSION_ID, "P-TODO82-SALES", 65, "RISK");
    UUID adminProjectId = insertProject(ADMIN_SUBMISSION_ID, "P-TODO82-ADMIN", 60, "RISK");
    linkOverdueWorkOrder(salesProjectId, SALES_SUBMISSION_ID, "SALES");
    linkOverdueWorkOrder(adminProjectId, ADMIN_SUBMISSION_ID, "ADMIN");
    insertBlocker(salesProjectId, "Sales visible blocker");
    insertBlocker(adminProjectId, "Admin visible blocker");

    mockMvc.perform(get("/work-center/projects")
            .param("filter", "risk")
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.items[0].healthStatus").value("RISK"));

    mockMvc.perform(get("/work-center/projects")
            .param("filter", "risk")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].projectBizNo").value("P-TODO82-SALES"));

    mockMvc.perform(get("/work-center/summary")
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.riskProjects").value(2))
        .andExpect(jsonPath("$.overdueTasks").value(2));

    mockMvc.perform(get("/work-center/summary")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.riskProjects").value(1))
        .andExpect(jsonPath("$.overdueTasks").value(1));

    mockMvc.perform(get("/work-center/resource-load")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].activeWorkOrders").value(1));

    mockMvc.perform(get("/work-center/trends")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.metricCode == 'riskProjects')]").isNotEmpty());
  }

  private UUID insertProject(UUID submissionId, String bizNo, int score, String healthStatus) {
    UUID projectId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into project (
          id, biz_no, submission_id, customer_org_id, project_type, qualification_type, service_type,
          requires_glp, requires_animal_test, requires_ethics_review, requires_nonstandard_review,
          business_owner_id, technical_owner_id, sd_user_id, status, health_status, health_score,
          payment_status, created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, '00000000-0000-0000-0000-000000000301',
          'STD_ACCREDITED', 'CMA', 'NORMAL',
          false, false, false, false, ?, ?, ?,
          'IN_TESTING', ?, ?, 'UNPAID', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        projectId,
        bizNo,
        submissionId,
        BUSINESS_ID,
        TECH_ID,
        SD_ID,
        healthStatus,
        score,
        ADMIN_ID,
        ADMIN_ID
    );
    return projectId;
  }

  private void linkOverdueWorkOrder(UUID projectId, UUID submissionId, String suffix) {
    UUID sampleId = insertSample(projectId, submissionId, suffix);
    UUID planId = insertStudyPlan(projectId, suffix);
    jdbcTemplate.update(
        """
        insert into work_order (
          id, biz_no, study_plan_id, project_id, sample_id, owner_user_id, status,
          progress_percent, last_progress_at, planned_finish_at, created_at, updated_at,
          version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, 'IN_PROGRESS', 50,
          now() - interval '1 day', now() - interval '1 hour',
          now(), now(), 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        "WO-TODO82-" + suffix,
        planId,
        projectId,
        sampleId,
        INSPECTOR_ID
    );
  }

  private UUID insertSample(UUID projectId, UUID submissionId, String suffix) {
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
        ) values (?, ?, ?, ?,
          'Todo 82 Sample', 'Todo 82 Sample', 'NON_STERILE', 'N/A', 'MODEL-82',
          '10 mm', ?, 'Polymer', 'Pouch', 'SOLID', 'White', 'N/A',
          'Stable', 'Insoluble', 'ROOM_TEMPERATURE', 'Validation sample',
          'No additional info', 10, 'pcs', 'IN_TESTING',
          '2026-05-14T00:00:00Z', '2026-05-14', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        sampleId,
        "SAMPLE-TODO82-" + suffix,
        projectId,
        submissionId,
        "LOT-" + suffix,
        ADMIN_ID,
        ADMIN_ID
    );
    return sampleId;
  }

  private UUID insertStudyPlan(UUID projectId, String suffix) {
    UUID planId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into study_plan (
          id, biz_no, project_id, status, requires_conditioning, requires_glp,
          requires_animal_test, sd_approved_at, created_at, updated_at, version,
          source_type, data_quality_score
        ) values (?, ?, ?, 'DISPATCHED', false, false, false, now(), now(), now(), 0, 'NEW', 100)
        """,
        planId,
        "PLAN-TODO82-" + suffix,
        projectId
    );
    return planId;
  }

  private void insertBlocker(UUID projectId, String description) {
    jdbcTemplate.update(
        """
        insert into work_center_blocker (
          id, project_id, blocker_type, severity, description, impact_scope,
          expected_recovery_at, owner_user_id, collaborator_ids, status, created_at,
          created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 'TECHNICAL_RISK', 'HIGH', ?, 'Schedule risk',
          now() + interval '1 day', ?, '[]'::jsonb, 'OPEN', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        projectId,
        description,
        INSPECTOR_ID,
        ADMIN_ID,
        ADMIN_ID
    );
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
