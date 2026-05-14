package com.njxs.lims.sample.retention;

import static org.assertj.core.api.Assertions.assertThat;
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
class SampleRetentionTests {

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
  SampleRetentionTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void registersExtendsAndDisposesRetentionBehindPaymentGate() throws Exception {
    String applicantToken = login("portal", "portal.applicant", "Applicant123!");
    String salesToken = login("internal", "internal.sales", "Sales123!");
    String sampleToken = login("internal", "internal.sample", "Sales123!");
    String qualityToken = login("internal", "internal.admin", "Internal123!");

    UUID submissionId = createDraft(applicantToken);
    UUID projectId = createProject(submissionId, applicantToken, salesToken);
    UUID sampleId = jdbcTemplate.queryForObject(
        "select id from sample where project_id = ?",
        UUID.class,
        projectId
    );
    assertThat(sampleId).isNotNull();

    String created = mockMvc.perform(post("/sample-retentions")
            .header(HttpHeaders.AUTHORIZATION, bearer(sampleToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sampleId": "%s",
                  "projectId": "%s",
                  "retainedQuantity": 2.5,
                  "quantityUnit": "pcs",
                  "storageCondition": "ROOM_TEMPERATURE",
                  "storageLocation": "RET-A-01",
                  "retainUntil": "2026-08-01",
                  "dispositionPlan": "DESTROY"
                }
                """.formatted(sampleId, projectId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_REGISTER"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID retentionId = UUID.fromString(objectMapper.readTree(created).get("id").asText());

    mockMvc.perform(post("/sample-retentions/{id}/register", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(sampleToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RETAINED"));

    mockMvc.perform(post("/sample-retentions/{id}/extend", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(sampleToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"retainUntil": "2026-10-01"}
                """))
        .andExpect(status().isBadRequest());

    mockMvc.perform(post("/sample-retentions/{id}/extend", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(sampleToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "retainUntil": "2026-10-01",
                  "reason": "客户要求延长观察期"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RETAINED"))
        .andExpect(jsonPath("$.retainUntil").value("2026-10-01"));

    String extensionReason = jdbcTemplate.queryForObject(
        "select extension_reason from sample_retention where id = ?",
        String.class,
        retentionId
    );
    assertThat(extensionReason).isEqualTo("客户要求延长观察期");

    mockMvc.perform(post("/sample-retentions/{id}/dispose", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "disposition": "DESTROY",
                  "reason": "到期销毁"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SAMPLE_RETENTION_PAYMENT_LOCKED"));

    jdbcTemplate.update("update project set payment_status = 'PAID' where id = ?", projectId);

    mockMvc.perform(post("/sample-retentions/{id}/dispose", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(sampleToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "disposition": "DESTROY",
                  "reason": "到期销毁"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DESTROYED"))
        .andExpect(jsonPath("$.dispositionPlan").value("DESTROY"));

    String dispositionReason = jdbcTemplate.queryForObject(
        "select disposition_reason from sample_retention where id = ?",
        String.class,
        retentionId
    );
    assertThat(dispositionReason).isEqualTo("到期销毁");
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
    data.put("sponsorNameZh", "Todo 47 Sponsor");
    data.put("sponsorNameEn", "Todo 47 Sponsor");
    data.put("sponsorAddressZh", "No. 47 Sponsor Road");
    data.put("sponsorAddressEn", "No. 47 Sponsor Road");
    data.put("manufacturerNameZh", "Todo 47 Manufacturer");
    data.put("manufacturerNameEn", "Todo 47 Manufacturer");
    data.put("manufacturerAddressZh", "No. 47 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 47 Manufacturer Road");
    data.put("contactName", "Todo Contact");
    data.put("contactPhone", "13800000047");
    data.put("contactEmail", "todo47@example.com");
    data.put("officialReportEmail", "reports.todo47@example.com");
    data.put("shippingInfo", "Ship to Todo 47 Sponsor");
    data.put("requiresGlp", false);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Todo 47 Sample");
    data.put("sampleNameEn", "Todo 47 Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-47");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-47");
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
