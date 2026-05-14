package com.njxs.lims.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SubmissionDraftFlowTests {

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

  @Autowired
  SubmissionDraftFlowTests(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  @Test
  void draftSubmitValidatesAgainstPublishedSchemaAndSalesCanListSubmittedItems() throws Exception {
    String applicantToken = login("portal", "portal.applicant", "Applicant123!");

    ObjectNode missingOfficialEmail = completeSubmissionData();
    missingOfficialEmail.remove("officialReportEmail");
    UUID invalidDraftId = createDraft(applicantToken, missingOfficialEmail);

    String validationResponse = mockMvc.perform(post("/submissions/{id}/submit", invalidDraftId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode missingFields = objectMapper.readTree(validationResponse).at("/details/missingFields");
    assertThat(StreamSupport.stream(missingFields.spliterator(), false).map(JsonNode::asText).toList())
        .contains("officialReportEmail");

    UUID validDraftId = createDraft(applicantToken, completeSubmissionData());
    mockMvc.perform(post("/submissions/{id}/submit", validDraftId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(validDraftId.toString()))
        .andExpect(jsonPath("$.status").value("SUBMITTED"));

    String salesToken = login("internal", "internal.sales", "Sales123!");
    String listResponse = mockMvc.perform(get("/submissions")
            .queryParam("page", "1")
            .queryParam("pageSize", "20")
            .header(HttpHeaders.AUTHORIZATION, bearer(salesToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").isNumber())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode items = objectMapper.readTree(listResponse).get("items");
    assertThat(StreamSupport.stream(items.spliterator(), false)
            .map(item -> item.get("id").asText())
            .toList())
        .contains(validDraftId.toString());
  }

  private UUID createDraft(String accessToken, ObjectNode data) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("formCode", "SUBMISSION_VER15");
    request.put("schemaVersion", 1);
    request.set("data", data);

    String response = mockMvc.perform(post("/submissions")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get("id").asText());
  }

  private ObjectNode completeSubmissionData() {
    ObjectNode data = objectMapper.createObjectNode();
    data.put("sponsorNameZh", "Todo 22 Sponsor");
    data.put("sponsorNameEn", "Todo 22 Sponsor");
    data.put("sponsorAddressZh", "No. 22 Sponsor Road");
    data.put("sponsorAddressEn", "No. 22 Sponsor Road");
    data.put("manufacturerNameZh", "Todo 22 Manufacturer");
    data.put("manufacturerNameEn", "Todo 22 Manufacturer");
    data.put("manufacturerAddressZh", "No. 22 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 22 Manufacturer Road");
    data.put("contactName", "Todo Contact");
    data.put("contactPhone", "13800000022");
    data.put("contactEmail", "todo22@example.com");
    data.put("officialReportEmail", "reports.todo22@example.com");
    data.put("shippingInfo", "Ship to Todo 22 Sponsor");
    data.put("requiresGlp", false);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Todo 22 Sample");
    data.put("sampleNameEn", "Todo 22 Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-22");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-22");
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
    return objectMapper.readTree(response).get("accessToken").asText();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
