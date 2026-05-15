package com.njxs.lims.workcenter;

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
class WorkCenterBlockerTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID INSPECTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000107");

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
  WorkCenterBlockerTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void closeRequiresResolutionAndRecalculatesProjectHealth() throws Exception {
    String qualityToken = login("internal", "internal.admin", "Internal123!");
    UUID projectId = insertProject();

    MvcResult createResult = mockMvc.perform(post("/work-center/blockers")
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "blockerType": "TECHNICAL_RISK",
                  "severity": "HIGH",
                  "description": "Unexpected positive control result",
                  "impactScope": "Sterility work order cannot continue",
                  "expectedRecoveryAt": "2026-05-15T08:00:00Z",
                  "ownerUserId": "%s",
                  "collaboratorIds": ["%s"]
                }
                """.formatted(projectId, INSPECTOR_ID, ADMIN_ID)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId.toString()))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.severity").value("HIGH"))
        .andReturn();
    UUID blockerId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
        .get("id")
        .asText());
    assertProjectHealth(projectId, 90, "RISK");

    mockMvc.perform(post("/work-center/blockers/{id}/close", blockerId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"resolution":" "}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BLOCKER_RESOLUTION_REQUIRED"));

    mockMvc.perform(post("/work-center/blockers/{id}/close", blockerId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"resolution":"Retested positive control and released the work order"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.resolution").value("Retested positive control and released the work order"));

    mockMvc.perform(post("/work-center/blockers/{id}/close", blockerId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"resolution":"Duplicate close"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BLOCKER_STATUS_CONFLICT"));

    assertProjectHealth(projectId, 100, "NORMAL");

    mockMvc.perform(get("/work-center/blockers")
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(blockerId.toString()))
        .andExpect(jsonPath("$.total").value(1));
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
          'IN_TESTING', 'NORMAL', 100, 'UNPAID', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        projectId,
        "P-TODO81-" + projectId,
        ADMIN_ID,
        ADMIN_ID
    );
    return projectId;
  }

  private void assertProjectHealth(UUID projectId, int score, String status) {
    Map<String, Object> health = jdbcTemplate.queryForMap(
        "select health_score, health_status from project where id = ?",
        projectId
    );
    assertThat(health)
        .containsEntry("health_score", score)
        .containsEntry("health_status", status);
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
