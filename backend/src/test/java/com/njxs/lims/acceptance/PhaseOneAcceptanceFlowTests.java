package com.njxs.lims.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
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
class PhaseOneAcceptanceFlowTests {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID SAMPLE_MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000105");
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
  PhaseOneAcceptanceFlowTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void fixedPhaseOneScenarioCompletesWithStateMachineStatusesAndAuditTrail() throws Exception {
    String applicantToken = login("portal", "portal.applicant", "Applicant123!");
    String salesToken = login("internal", "internal.sales", "Sales123!");
    String techToken = login("internal", "internal.tech", "Sales123!");
    String businessToken = login("internal", "internal.business", "Sales123!");
    String sampleToken = login("internal", "internal.sample", "Sales123!");
    String sdToken = login("internal", "internal.sd", "Sales123!");
    String inspectorToken = login("internal", "internal.inspector", "Sales123!");
    String qcToken = login("internal", "internal.qc", "Sales123!");
    String qaToken = login("internal", "internal.qa", "Sales123!");
    String reportPreparerToken = login("internal", "internal.report_preparer", "Sales123!");
    String signatoryToken = login("internal", "internal.authorized_signatory", "Sales123!");
    String financeToken = login("internal", "internal.finance", "Sales123!");
    String adminToken = login("internal", "internal.admin", "Internal123!");

    UUID submissionId = createSubmission(applicantToken);
    submitSubmission(submissionId, applicantToken);
    acceptSubmission(submissionId, salesToken);

    UUID projectId = splitProject(submissionId, salesToken);
    UUID sampleId = onlySample(projectId);
    assertEntityStatus("submission", submissionId, "PROJECT_SPLIT");
    assertEntityStatus("project", projectId, "ACCEPTED");
    assertEntityStatus("sample", sampleId, "WAITING_DELIVERY");

    startNonstandardReview(projectId, salesToken);
    approveNonstandard(projectId, techToken, "TECH");
    approveNonstandard(projectId, businessToken, "BUSINESS");
    assertEntityStatus("project", projectId, "QUOTABLE");

    createQuotation(projectId, salesToken);
    assertEntityStatus("project", projectId, "QUOTED");

    UUID contractId = createContract(projectId, salesToken);
    signContract(contractId, salesToken);
    assertEntityStatus("project", projectId, "CONTRACTED");

    receiveAndStockInSample(sampleId, sampleToken);
    assertEntityStatus("sample", sampleId, "IN_STOCK");
    assertEntityStatus("project", projectId, "SAMPLE_RECEIVED");

    UUID planId = createAndApproveStudyPlan(projectId, techToken, sdToken);
    assertEntityStatus("study_plan", planId, "READY_TO_DISPATCH");
    assertEntityStatus("project", projectId, "PLAN_READY");

    dispatchStudyPlan(planId, sdToken);
    UUID workOrderId = onlyWorkOrder(planId);
    assertWorkOrder(workOrderId, "PENDING_EXECUTION", INSPECTOR_ID);
    assertEntityStatus("project", projectId, "IN_TESTING");

    executeWorkOrder(workOrderId, inspectorToken);
    UUID rawRecordId = submitAndApproveRawRecord(workOrderId, inspectorToken, qcToken);
    assertEntityStatus("raw_record", rawRecordId, "APPROVED");
    assertWorkOrder(workOrderId, "COMPLETED", INSPECTOR_ID);
    assertEntityStatus("project", projectId, "REPORTING");

    UUID templateId = createAndPublishReportTemplate(adminToken);
    UUID reportId = generateAndIssueReport(projectId, workOrderId, reportPreparerToken, qcToken, qaToken, sdToken, signatoryToken);
    assertThat(templateId).isEqualTo(reportTemplateId(reportId));
    assertEntityStatus("report", reportId, "ISSUED_PAYMENT_LOCKED");
    assertEntityStatus("project", projectId, "PAYMENT_HOLD");

    mockMvc.perform(get("/reports/{id}/official-pdf", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isLocked())
        .andExpect(jsonPath("$.code").value("PAYMENT_GATE_LOCKED"));

    recordPayment(projectId, reportId, financeToken);
    assertPaymentAndReportReleased(projectId, reportId);

    MvcResult download = mockMvc.perform(get("/reports/{id}/official-pdf", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(download.getResponse().getContentType()).isEqualTo("application/pdf");
    assertThat(download.getResponse().getContentAsByteArray()).isNotEmpty();

    UUID retentionId = retainExtendAndDisposeSample(sampleId, projectId, reportId, sampleToken);
    assertEntityStatus("sample_retention", retentionId, "DESTROYED");

    String workCenterResponse = mockMvc.perform(get("/work-center/projects")
            .param("page", "1")
            .param("pageSize", "50")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    assertWorkCenterProject(workCenterResponse, projectId);

    assertAuditActions(List.of(
        "SUBMISSION_CREATE",
        "SUBMISSION_ACCEPT",
        "PROJECT_TYPE_DECIDE",
        "NONSTANDARD_REVIEW_START",
        "NONSTANDARD_TECH_APPROVE",
        "NONSTANDARD_BUSINESS_APPROVE",
        "QUOTATION_CREATE",
        "CONTRACT_CREATE",
        "CONTRACT_SIGN_CONFIRM",
        "SAMPLE_RECEIVE",
        "SAMPLE_STOCK_IN",
        "PLAN_CREATE",
        "PLAN_REVIEW_SD",
        "PLAN_DISPATCH",
        "WORK_ORDER_EXECUTE",
        "RAW_RECORD_SUBMIT",
        "RAW_RECORD_REVIEW",
        "REPORT_TEMPLATE_UPLOAD",
        "REPORT_TEMPLATE_PUBLISH",
        "REPORT_DRAFT_CREATE",
        "REPORT_DRAFT_SUBMIT_QC",
        "REPORT_QC_REVIEW",
        "REPORT_QA_REVIEW",
        "REPORT_SD_REVIEW",
        "REPORT_ISSUE",
        "PAYMENT_RECEIPT_CREATE",
        "SAMPLE_RETENTION_REGISTER",
        "SAMPLE_RETENTION_EXTEND",
        "SAMPLE_RETENTION_DISPOSE"
    ));
  }

  private UUID createSubmission(String token) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("formCode", "SUBMISSION_VER15");
    request.put("schemaVersion", 1);
    request.set("data", completeSubmissionData());
    String response = mockMvc.perform(post("/submissions")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return id(response);
  }

  private void submitSubmission(UUID submissionId, String token) throws Exception {
    mockMvc.perform(post("/submissions/{id}/submit", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  private void acceptSubmission(UUID submissionId, String token) throws Exception {
    mockMvc.perform(post("/submissions/{id}/accept", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"));
  }

  private UUID splitProject(UUID submissionId, String token) throws Exception {
    String response = mockMvc.perform(post("/submissions/{id}/split-projects", submissionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
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
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).get(0).get("id").asText());
  }

  private void startNonstandardReview(UUID projectId, String token) throws Exception {
    mockMvc.perform(post("/projects/{id}/nonstandard-review/start", projectId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NONSTANDARD_REVIEW"));
  }

  private void approveNonstandard(UUID projectId, String token, String stage) throws Exception {
    mockMvc.perform(post("/projects/{id}/nonstandard-review/approve", projectId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stage": "%s",
                  "outcome": "APPROVED",
                  "comment": "Phase one acceptance"
                }
                """.formatted(stage)))
        .andExpect(status().isOk());
  }

  private void createQuotation(UUID projectId, String token) throws Exception {
    mockMvc.perform(post("/projects/{id}/quotations", projectId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "items": [
                    {
                      "testName": "Sterility",
                      "unitPrice": 1200.00,
                      "quantity": 1
                    }
                  ],
                  "discountAmount": 0.00,
                  "note": "Phase one acceptance quote"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId.toString()));
  }

  private UUID createContract(UUID projectId, String token) throws Exception {
    String response = mockMvc.perform(post("/contracts")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
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
    return id(response);
  }

  private void signContract(UUID contractId, String token) throws Exception {
    mockMvc.perform(post("/contracts/{id}/mark-signed", contractId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"signedAt": "2026-05-14T16:00:00Z"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SIGNED"));
  }

  private void receiveAndStockInSample(UUID sampleId, String token) throws Exception {
    mockMvc.perform(post("/samples/{id}/receive", sampleId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "receivedAt": "2026-05-14T16:30:00Z",
                  "receivedBy": "%s",
                  "checkPassed": true
                }
                """.formatted(SAMPLE_MANAGER_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PRE_IN_STOCK"));

    mockMvc.perform(post("/samples/{id}/stock-in", sampleId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "inventoryLocation": "ACCEPTANCE-A-01",
                  "stockedAt": "2026-05-14T17:00:00Z"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_STOCK"));
  }

  private UUID createAndApproveStudyPlan(UUID projectId, String techToken, String sdToken) throws Exception {
    String response = mockMvc.perform(post("/study-plans")
            .header(HttpHeaders.AUTHORIZATION, bearer(techToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "requiresConditioning": false,
                  "plannedStartAt": "2026-05-20T00:00:00Z",
                  "plannedFinishAt": "2026-05-30T00:00:00Z"
                }
                """.formatted(projectId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID planId = id(response);

    mockMvc.perform(post("/study-plans/{id}/submit-review", planId)
            .header(HttpHeaders.AUTHORIZATION, bearer(sdToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY_TO_DISPATCH"));
    return planId;
  }

  private void dispatchStudyPlan(UUID planId, String token) throws Exception {
    mockMvc.perform(post("/study-plans/{id}/dispatch", planId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISPATCHED"));
  }

  private void executeWorkOrder(UUID workOrderId, String token) throws Exception {
    mockMvc.perform(post("/work-orders/{id}/pick-sample", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SAMPLE_PICKED"));
    mockMvc.perform(post("/work-orders/{id}/start", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    mockMvc.perform(post("/work-orders/{id}/enter-result", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESULT_ENTERED"));
  }

  private UUID submitAndApproveRawRecord(UUID workOrderId, String inspectorToken, String qcToken) throws Exception {
    String response = mockMvc.perform(post("/work-orders/{id}/submit-raw-record", workOrderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(inspectorToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawRecordPayload()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVIEWING"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID rawRecordId = id(response);

    mockMvc.perform(post("/raw-records/{id}/review", rawRecordId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qcToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stage": "QC",
                  "outcome": "APPROVED",
                  "comment": "Phase one raw record approved"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    return rawRecordId;
  }

  private UUID createAndPublishReportTemplate(String token) throws Exception {
    UUID fileId = insertFile("phase-one-sterility-template.docx");
    String response = mockMvc.perform(post("/report-templates")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "STERILITY_REPORT",
                  "templateVersion": "92.0",
                  "templateName": "Phase One Sterility Report",
                  "templateType": "REPORT",
                  "automationLevel": "A",
                  "fileId": "%s",
                  "legacyTemplateName": "phase-one-sterility"
                }
                """.formatted(fileId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID templateId = id(response);
    mockMvc.perform(post("/report-templates/{id}/publish", templateId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
    return templateId;
  }

  private UUID generateAndIssueReport(
      UUID projectId,
      UUID workOrderId,
      String preparerToken,
      String qcToken,
      String qaToken,
      String sdToken,
      String signatoryToken
  ) throws Exception {
    String response = mockMvc.perform(post("/reports/generate-draft")
            .header(HttpHeaders.AUTHORIZATION, bearer(preparerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "templateCode": "STERILITY_REPORT",
                  "workOrderIds": ["%s"]
                }
                """.formatted(projectId, workOrderId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID reportId = id(response);

    mockMvc.perform(post("/reports/{id}/submit-qc", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(preparerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QC_REVIEWING"));
    reviewReport(reportId, qcToken, "QC", "QA_REVIEWING");
    reviewReport(reportId, qaToken, "QA", "SD_REVIEWING");
    reviewReport(reportId, sdToken, "SD", "PENDING_ISSUE");
    mockMvc.perform(post("/reports/{id}/issue", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(signatoryToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ISSUED_PAYMENT_LOCKED"));
    return reportId;
  }

  private void reviewReport(UUID reportId, String token, String stage, String expectedStatus) throws Exception {
    mockMvc.perform(post("/reports/{id}/review", reportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "stage": "%s",
                  "outcome": "APPROVED",
                  "comment": "Phase one report review"
                }
                """.formatted(stage)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(expectedStatus));
  }

  private void recordPayment(UUID projectId, UUID reportId, String token) throws Exception {
    mockMvc.perform(post("/payment-receipts")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "projectId": "%s",
                  "reportId": "%s",
                  "amount": 1200.00,
                  "receivedAt": "2026-05-14T18:00:00Z",
                  "method": "BANK_TRANSFER",
                  "note": "Phase one acceptance payment"
                }
                """.formatted(projectId, reportId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("RECORDED"));
  }

  private UUID retainExtendAndDisposeSample(UUID sampleId, UUID projectId, UUID reportId, String token) throws Exception {
    String response = mockMvc.perform(post("/sample-retentions")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "sampleId": "%s",
                  "projectId": "%s",
                  "reportId": "%s",
                  "retainedQuantity": 2.0,
                  "quantityUnit": "pcs",
                  "storageCondition": "ROOM_TEMPERATURE",
                  "storageLocation": "RET-PHASE-ONE",
                  "retainUntil": "2026-08-01",
                  "dispositionPlan": "DESTROY"
                }
                """.formatted(sampleId, projectId, reportId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_REGISTER"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    UUID retentionId = id(response);

    mockMvc.perform(post("/sample-retentions/{id}/register", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RETAINED"));
    mockMvc.perform(post("/sample-retentions/{id}/extend", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "retainUntil": "2026-10-01",
                  "reason": "Phase one retention extension"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retainUntil").value("2026-10-01"));
    mockMvc.perform(post("/sample-retentions/{id}/dispose", retentionId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "disposition": "DESTROY",
                  "reason": "Phase one disposal"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DESTROYED"));
    return retentionId;
  }

  private ObjectNode completeSubmissionData() {
    ObjectNode data = objectMapper.createObjectNode();
    data.put("sponsorNameZh", "Phase One Sponsor");
    data.put("sponsorNameEn", "Phase One Sponsor");
    data.put("sponsorAddressZh", "No. 92 Sponsor Road");
    data.put("sponsorAddressEn", "No. 92 Sponsor Road");
    data.put("manufacturerNameZh", "Phase One Manufacturer");
    data.put("manufacturerNameEn", "Phase One Manufacturer");
    data.put("manufacturerAddressZh", "No. 92 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 92 Manufacturer Road");
    data.put("contactName", "Phase One Contact");
    data.put("contactPhone", "13800000092");
    data.put("contactEmail", "phase92@example.com");
    data.put("officialReportEmail", "reports.phase92@example.com");
    data.put("shippingInfo", "Ship to phase one sponsor");
    data.put("requiresGlp", false);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Phase One Sterility Sample");
    data.put("sampleNameEn", "Phase One Sterility Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-92");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-92");
    data.put("rawMaterial", "Polymer");
    data.put("packagingMaterial", "Pouch");
    data.put("physicalState", "SOLID");
    data.put("color", "White");
    data.put("density", "N/A");
    data.put("stability", "Stable");
    data.put("solubility", "Insoluble");
    data.put("storageCondition", "ROOM_TEMPERATURE");
    data.put("intendedUse", "Phase one validation sample");
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

  private String rawRecordPayload() {
    return """
        {
          "templateCode": "RAW_RECORD_STERILITY",
          "recordData": {
            "testItem": "STERILITY",
            "testBasis": "ISO 11737",
            "testTime": "2026-05-14",
            "testLocation": "Lab A",
            "temperatureCelsius": "22",
            "humidityPercent": "45",
            "method": "DIRECT_INOCULATION",
            "conclusion": "No growth observed",
            "equipmentUsages": [
              {"name": "Incubator", "no": "EQ-92"}
            ],
            "sterilityResults": [
              {"sampleName": "Phase One Sterility Sample", "conclusion": "无菌"}
            ]
          },
          "attachmentFileIds": []
        }
        """;
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
        "acceptance/" + fileId + "/" + fileName,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private UUID onlySample(UUID projectId) {
    return jdbcTemplate.queryForObject(
        "select id from sample where project_id = ?",
        UUID.class,
        projectId
    );
  }

  private UUID onlyWorkOrder(UUID planId) {
    return jdbcTemplate.queryForObject(
        "select id from work_order where study_plan_id = ?",
        UUID.class,
        planId
    );
  }

  private UUID reportTemplateId(UUID reportId) {
    return jdbcTemplate.queryForObject(
        "select report_template_id from report where id = ?",
        UUID.class,
        reportId
    );
  }

  private void assertEntityStatus(String tableName, UUID id, String expectedStatus) {
    String status = jdbcTemplate.queryForObject(
        "select status from " + tableName + " where id = ?",
        String.class,
        id
    );
    assertThat(status).isEqualTo(expectedStatus);
  }

  private void assertWorkOrder(UUID id, String expectedStatus, UUID expectedOwnerId) {
    Map<String, Object> workOrder = jdbcTemplate.queryForMap(
        "select status, owner_user_id from work_order where id = ?",
        id
    );
    assertThat(workOrder)
        .containsEntry("status", expectedStatus)
        .containsEntry("owner_user_id", expectedOwnerId);
  }

  private void assertPaymentAndReportReleased(UUID projectId, UUID reportId) {
    Map<String, Object> state = jdbcTemplate.queryForMap(
        """
        select p.status project_status, p.payment_status, r.status report_status, r.payment_gate_status
        from project p
        join report r on r.project_id = p.id
        where p.id = ? and r.id = ?
        """,
        projectId,
        reportId
    );
    assertThat(state)
        .containsEntry("project_status", "RELEASED")
        .containsEntry("payment_status", "PAID")
        .containsEntry("report_status", "RELEASED")
        .containsEntry("payment_gate_status", "UNLOCKED");
  }

  private void assertWorkCenterProject(String response, UUID projectId) throws Exception {
    JsonNode item = null;
    for (JsonNode candidate : objectMapper.readTree(response).get("items")) {
      if (projectId.toString().equals(candidate.path("projectId").asText())) {
        item = candidate;
        break;
      }
    }
    assertThat(item).as("project in work center").isNotNull();
    assertThat(item.get("healthStatus").asText()).isEqualTo("NORMAL");
    assertThat(item.get("status").asText()).isEqualTo("RELEASED");
  }

  private void assertAuditActions(List<String> actionCodes) {
    for (String actionCode : actionCodes) {
      Integer count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from audit_log
          where action_code = ?
            and result = 'SUCCESS'
          """,
          Integer.class,
          actionCode
      );
      assertThat(count).as("audit action %s", actionCode).isNotNull().isGreaterThan(0);
    }
  }

  private UUID id(String json) throws Exception {
    return UUID.fromString(objectMapper.readTree(json).get("id").asText());
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
