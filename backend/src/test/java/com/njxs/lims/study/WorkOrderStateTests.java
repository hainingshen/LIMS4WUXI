package com.njxs.lims.study;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class WorkOrderStateTests {

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
  WorkOrderStateTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void onlyOwnerCanStartAndSamplePickedMustPrecedeInProgress() throws Exception {
    String applicantToken = login("portal", "portal.applicant", "Applicant123!");
    String salesToken = login("internal", "internal.sales", "Sales123!");
    String ownerToken = login("internal", "internal.inspector", "Sales123!");
    String otherInspectorToken = login("internal", "internal.inspector2", "Sales123!");

    UUID submissionId = createDraft(applicantToken);
    UUID projectId = createProject(submissionId, applicantToken, salesToken);
    UUID sampleId = jdbcTemplate.queryForObject(
        "select id from sample where project_id = ?",
        UUID.class,
        projectId
    );
    UUID planId = insertStudyPlan(projectId);
    UUID workOrderId = insertWorkOrder(planId, projectId, sampleId);

    mockMvc.perform(post("/work-orders/{id}/start", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(otherInspectorToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.details.actionCode").value("WORK_ORDER_EXECUTE"));

    mockMvc.perform(post("/work-orders/{id}/start", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("WORK_ORDER_STATUS_CONFLICT"))
        .andExpect(jsonPath("$.details.actual").value("PENDING_EXECUTION"));

    jdbcTemplate.update("update work_order set status = 'SAMPLE_PICKED' where id = ?", workOrderId);

    mockMvc.perform(post("/work-orders/{id}/start", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.progressPercent").value(10));
  }

  private UUID insertStudyPlan(UUID projectId) {
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
        "PLAN-TODO51-" + UUID.randomUUID(),
        projectId
    );
    return planId;
  }

  private UUID insertWorkOrder(UUID planId, UUID projectId, UUID sampleId) {
    UUID workOrderId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into work_order (
          id, biz_no, study_plan_id, project_id, sample_id, owner_user_id, status,
          progress_percent, created_at, updated_at, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, 'PENDING_EXECUTION', 0, now(), now(), 0, 'NEW', 100)
        """,
        workOrderId,
        "WO-TODO51-" + UUID.randomUUID(),
        planId,
        projectId,
        sampleId,
        INSPECTOR_ID
    );
    return workOrderId;
  }

  private UUID createProject(UUID submissionId, String applicantToken, String salesToken) throws Exception {
    mockMvc.perform(post("/submissions/{id}/submit", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/submissions/{id}/accept", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk());

    String splitResponse = mockMvc.perform(post("/submissions/{id}/split-projects", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "splitStrategy": "MANUAL",
                  "projectDrafts": [
                    {
                      "projectType": "STD_ACCREDITED",
                      "qualificationType": "CMA",
                      "serviceType": "NORMAL"
                    }
                  ]
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(splitResponse).get(0).get("id").asText());
  }

  private UUID createDraft(String accessToken) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("formCode", "SUBMISSION_VER15");
    request.put("schemaVersion", 1);
    request.set("data", completeSubmissionData());

    String response = mockMvc.perform(post("/submissions")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private ObjectNode completeSubmissionData() {
    ObjectNode data = objectMapper.createObjectNode();
    data.put("sponsorNameZh", "Todo 51 Sponsor");
    data.put("sponsorNameEn", "Todo 51 Sponsor");
    data.put("sponsorAddressZh", "No. 51 Sponsor Road");
    data.put("sponsorAddressEn", "No. 51 Sponsor Road");
    data.put("manufacturerNameZh", "Todo 51 Manufacturer");
    data.put("manufacturerNameEn", "Todo 51 Manufacturer");
    data.put("manufacturerAddressZh", "No. 51 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 51 Manufacturer Road");
    data.put("contactName", "Todo Contact");
    data.put("contactPhone", "13800000051");
    data.put("contactEmail", "todo51@example.com");
    data.put("officialReportEmail", "reports.todo51@example.com");
    data.put("shippingInfo", "Ship to Todo 51 Sponsor");
    data.put("requiresGlp", false);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Todo 51 Sample");
    data.put("sampleNameEn", "Todo 51 Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-51");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-51");
    data.put("rawMaterial", "Polymer");
    data.put("packagingMaterial", "Pouch");
    data.put("physicalState", "SOLID");
    data.put("color", "White");
    data.put("density", "N/A");
    data.put("stability", "Stable");
    data.put("solubility", "Insoluble");
    data.put("storageCondition", "ROOM_TEMPERATURE");
    data.put("intendedUse", "Validation sample");
    data.put("additionalInfo", "No additional info");
    data.put("canBeCut", true);
    data.put("componentsToTestZh", "N/A");
    data.put("componentsToTestEn", "N/A");
    data.put("workingTemperatureCelsius", "37");
    data.put("withstand50C", false);
    data.put("clinicalContactDuration", "A_LIMITED");
    data.put("extractionBasis", "SURFACE_AREA");
    data.put("thinnestThicknessMm", "1");
    data.put("specialInstructions", "N/A");
    return data;
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
