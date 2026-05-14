package com.njxs.lims.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
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
class ContractSignLockTests {

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
  ContractSignLockTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void signedContractMovesProjectToContractedAndLocksSubmissionChangeFields() throws Exception {
    String applicantToken = login("portal", "portal.applicant", "Applicant123!");
    String salesToken = login("internal", "internal.sales", "Sales123!");
    String techToken = login("internal", "internal.tech", "Sales123!");
    String businessToken = login("internal", "internal.business", "Sales123!");

    UUID submissionId = createDraft(applicantToken, completeSubmissionData());
    UUID projectId = createQuotedProject(submissionId, applicantToken, salesToken, techToken, businessToken);
    UUID contractId = createContract(projectId, salesToken);

    mockMvc.perform(post("/contracts/{id}/mark-signed", contractId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "signedAt": "2026-05-14T16:00:00Z"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SIGNED"))
        .andExpect(jsonPath("$.signedAt").value("2026-05-14T16:00:00Z"));

    String projectStatus = jdbcTemplate.queryForObject(
        "select status from project where id = ?",
        String.class,
        projectId
    );
    assertThat(projectStatus).isEqualTo("CONTRACTED");

    Timestamp changeLockedAt = jdbcTemplate.queryForObject(
        "select change_locked_at from submission where id = ?",
        Timestamp.class,
        submissionId
    );
    assertThat(changeLockedAt).isNotNull();

    ObjectNode changedData = completeSubmissionData();
    changedData.put("sponsorNameZh", "Todo 40 Sponsor Changed");
    mockMvc.perform(patch("/submissions/{id}", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(submissionRequest(changedData)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FIELD_CHANGE_REQUIRES_CHANGE_REQUEST"))
        .andExpect(jsonPath("$.details.fieldPath").value("sponsorNameZh"))
        .andExpect(jsonPath("$.details.nextAction").value("CREATE_CHANGE_REQUEST"));
  }

  private UUID createQuotedProject(
      UUID submissionId,
      String applicantToken,
      String salesToken,
      String techToken,
      String businessToken
  ) throws Exception {
    mockMvc.perform(post("/submissions/{id}/submit", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/submissions/{id}/accept", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk());

    String splitResponse = mockMvc.perform(post("/submissions/{id}/split-projects", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(splitRequest()))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID projectId = UUID.fromString(objectMapper.readTree(splitResponse).get(0).get("id").asText());

    mockMvc.perform(post("/projects/{id}/nonstandard-review/start", projectId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk());
    approve(projectId, techToken, "TECH");
    approve(projectId, businessToken, "BUSINESS");

    mockMvc.perform(post("/projects/{id}/quotations", projectId)
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(quotationRequest()))
        .andExpect(status().isCreated());
    return projectId;
  }

  private UUID createContract(UUID projectId, String salesToken) throws Exception {
    String response = mockMvc.perform(post("/contracts")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "contractType": "TEST_AGREEMENT",
                  "templateCode": "TEST_AGREEMENT_VER1"
                }
                """.formatted(projectId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_SIGN"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private void approve(UUID projectId, String token, String stage) throws Exception {
    mockMvc.perform(post("/projects/{projectId}/nonstandard-review/approve", projectId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stage": "%s",
                  "outcome": "APPROVED",
                  "comment": "通过"
                }
                """.formatted(stage)))
        .andExpect(status().isOk());
  }

  private UUID createDraft(String accessToken, ObjectNode data) throws Exception {
    String response = mockMvc.perform(post("/submissions")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(submissionRequest(data)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private String submissionRequest(ObjectNode data) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("formCode", "SUBMISSION_VER15");
    request.put("schemaVersion", 1);
    request.set("data", data);
    return objectMapper.writeValueAsString(request);
  }

  private String splitRequest() {
    return """
        {
          "splitStrategy": "MANUAL",
          "projectDrafts": [
            {
              "projectType": "NON_STANDARD",
              "qualificationType": "NONE",
              "serviceType": "NORMAL"
            }
          ]
        }
        """;
  }

  private String quotationRequest() {
    return """
        {
          "items": [
            {
              "testName": "Cytotoxicity",
              "unitPrice": 100.00,
              "quantity": 2
            }
          ],
          "discountAmount": 10.00,
          "note": "Todo 40 quote"
        }
        """;
  }

  private ObjectNode completeSubmissionData() {
    ObjectNode data = objectMapper.createObjectNode();
    data.put("sponsorNameZh", "Todo 40 Sponsor");
    data.put("sponsorNameEn", "Todo 40 Sponsor");
    data.put("sponsorAddressZh", "No. 40 Sponsor Road");
    data.put("sponsorAddressEn", "No. 40 Sponsor Road");
    data.put("manufacturerNameZh", "Todo 40 Manufacturer");
    data.put("manufacturerNameEn", "Todo 40 Manufacturer");
    data.put("manufacturerAddressZh", "No. 40 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 40 Manufacturer Road");
    data.put("contactName", "Todo Contact");
    data.put("contactPhone", "13800000040");
    data.put("contactEmail", "todo40@example.com");
    data.put("officialReportEmail", "reports.todo40@example.com");
    data.put("shippingInfo", "Ship to Todo 40 Sponsor");
    data.put("requiresGlp", true);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Todo 40 Sample");
    data.put("sampleNameEn", "Todo 40 Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-40");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-40");
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
