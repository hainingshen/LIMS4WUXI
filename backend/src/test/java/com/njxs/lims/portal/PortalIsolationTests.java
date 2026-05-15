package com.njxs.lims.portal;

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
class PortalIsolationTests {

  private static final UUID PORTAL_CUSTOMER_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000301");
  private static final UUID PORTAL_APPLICANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000202");
  private static final UUID ADMIN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000101");

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
  PortalIsolationTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void portalDraftUsesBoundCustomerOrg() throws Exception {
    String applicantToken = login("portal.applicant", "Applicant123!");

    mockMvc.perform(post("/submissions")
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(submissionRequest(completeSubmissionData())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerOrgId").value(PORTAL_CUSTOMER_ORG_ID.toString()))
        .andExpect(jsonPath("$.applicantUserId").value(PORTAL_APPLICANT_ID.toString()));
  }

  @Test
  void portalApplicantCannotListOrDownloadOwnRecordsOutsideBoundCustomerOrg() throws Exception {
    String applicantToken = login("portal.applicant", "Applicant123!");
    CrossCustomerFixture fixture = insertCrossCustomerFixture();

    assertListDoesNotContain("/submissions", fixture.submissionId(), applicantToken);
    assertListDoesNotContain("/projects", fixture.projectId(), applicantToken);
    assertListDoesNotContain("/contracts", fixture.contractId(), applicantToken);
    assertListDoesNotContain("/reports", fixture.reportId(), applicantToken);

    mockMvc.perform(get("/reports/{id}/official-pdf", fixture.reportId())
            .header(HttpHeaders.AUTHORIZATION, bearer(applicantToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.details.actionCode").value("REPORT_OFFICIAL_DOWNLOAD"));
  }

  @Test
  void portalViewerDoesNotFallBackToWholeOrgWithoutGrant() throws Exception {
    String viewerToken = login("portal.viewer", "Viewer123!");

    assertEmptyList("/submissions", viewerToken);
    assertEmptyList("/projects", viewerToken);
    assertEmptyList("/contracts", viewerToken);
    assertEmptyList("/reports", viewerToken);
  }

  private void assertListDoesNotContain(String path, UUID id, String token) throws Exception {
    String response = mockMvc.perform(get(path)
            .queryParam("page", "1")
            .queryParam("pageSize", "100")
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode items = objectMapper.readTree(response).path("items");
    assertThat(StreamSupport.stream(items.spliterator(), false)
            .map(item -> item.path("id").asText())
            .toList())
        .doesNotContain(id.toString());
  }

  private void assertEmptyList(String path, String token) throws Exception {
    mockMvc.perform(get(path)
            .queryParam("page", "1")
            .queryParam("pageSize", "100")
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.items").isEmpty());
  }

  private CrossCustomerFixture insertCrossCustomerFixture() {
    UUID otherOrgId = UUID.randomUUID();
    UUID contactId = UUID.randomUUID();
    UUID manufacturerId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();

    jdbcTemplate.update(
        """
        insert into customer_org (
          id, biz_no, name_zh, name_en, address_zh, address_en, status,
          created_at, created_by, updated_at, updated_by, version, tenant_org_id, source_type, data_quality_score
        ) values (?, ?, 'Cross Customer', 'Cross Customer', 'Cross Road', 'Cross Road', 'ACTIVE',
          now(), ?, now(), ?, 0, ?, 'NEW', 100)
        """,
        otherOrgId,
        "CUST-CROSS-" + otherOrgId,
        ADMIN_ID,
        ADMIN_ID,
        otherOrgId
    );
    jdbcTemplate.update(
        """
        insert into customer_contact (
          id, biz_no, customer_org_id, contact_name, phone, email, is_primary, status,
          created_at, created_by, updated_at, updated_by, version, tenant_org_id, source_type, data_quality_score
        ) values (?, ?, ?, 'Cross Contact', '13800000000', ?, true, 'ACTIVE',
          now(), ?, now(), ?, 0, ?, 'NEW', 100)
        """,
        contactId,
        "CONTACT-CROSS-" + contactId,
        otherOrgId,
        "cross-" + contactId + "@example.com",
        ADMIN_ID,
        ADMIN_ID,
        otherOrgId
    );
    jdbcTemplate.update(
        """
        insert into manufacturer (
          id, biz_no, name_zh, name_en, address_zh, address_en, status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, 'Cross Manufacturer', 'Cross Manufacturer', 'M Road', 'M Road', 'ACTIVE',
          now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        manufacturerId,
        "MFR-CROSS-" + manufacturerId,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into submission (
          id, biz_no, customer_org_id, customer_contact_id, manufacturer_id, applicant_user_id,
          assigned_sales_user_id, form_code, schema_version, data_json, status, official_report_email,
          shipping_info, requires_glp, requires_hard_copy_of_electronic_report, report_language,
          protocol_language, submitted_quantity_text, sample_disposition, created_at, created_by,
          updated_at, updated_by, version, tenant_org_id, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, '00000000-0000-0000-0000-000000000102',
          'SUBMISSION_VER15', 1, '{}', 'SUBMITTED', 'cross@example.com', 'Cross shipping',
          false, false, 'CHS', 'CHS', '1 pc', 'DESTROY_DISCARD', now(), ?, now(), ?, 0, ?, 'NEW', 100)
        """,
        submissionId,
        "SUB-CROSS-" + submissionId,
        otherOrgId,
        contactId,
        manufacturerId,
        PORTAL_APPLICANT_ID,
        PORTAL_APPLICANT_ID,
        PORTAL_APPLICANT_ID,
        otherOrgId
    );
    jdbcTemplate.update(
        """
        insert into project (
          id, biz_no, submission_id, customer_org_id, project_type, qualification_type, service_type,
          requires_glp, requires_animal_test, requires_ethics_review, requires_nonstandard_review,
          business_owner_id, technical_owner_id, sd_user_id, status, health_status, health_score,
          payment_status, created_at, created_by, updated_at, updated_by, version, tenant_org_id,
          source_type, data_quality_score
        ) values (?, ?, ?, ?, 'STD_ACCREDITED', 'CMA', 'NORMAL',
          false, false, false, false, '00000000-0000-0000-0000-000000000104',
          '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000106',
          'REPORTING', 'NORMAL', 100, 'PAID', now(), ?, now(), ?, 0, ?, 'NEW', 100)
        """,
        projectId,
        "P-CROSS-" + projectId,
        submissionId,
        otherOrgId,
        ADMIN_ID,
        ADMIN_ID,
        otherOrgId
    );
    jdbcTemplate.update(
        """
        insert into contract (
          id, biz_no, project_id, customer_org_id, contract_type, template_code, status,
          created_at, created_by, updated_at, updated_by, version, tenant_org_id, source_type, data_quality_score
        ) values (?, ?, ?, ?, 'TEST_AGREEMENT', 'TEST_AGREEMENT_VER1', 'PENDING_SIGN',
          now(), ?, now(), ?, 0, ?, 'NEW', 100)
        """,
        contractId,
        "CON-CROSS-" + contractId,
        projectId,
        otherOrgId,
        ADMIN_ID,
        ADMIN_ID,
        otherOrgId
    );
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, report_template_id, template_code, template_version, status,
          current_version_no, is_official, watermark_type, payment_gate_status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, null, 'STERILITY_REPORT', '1.0', 'RELEASED',
          1, true, 'NONE', 'UNLOCKED', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        reportId,
        "RPT-CROSS-" + reportId,
        projectId,
        ADMIN_ID,
        ADMIN_ID
    );

    return new CrossCustomerFixture(submissionId, projectId, contractId, reportId);
  }

  private String submissionRequest(ObjectNode data) throws Exception {
    ObjectNode request = objectMapper.createObjectNode();
    request.put("formCode", "SUBMISSION_VER15");
    request.put("schemaVersion", 1);
    request.set("data", data);
    return objectMapper.writeValueAsString(request);
  }

  private ObjectNode completeSubmissionData() {
    ObjectNode data = objectMapper.createObjectNode();
    data.put("sponsorNameZh", "Portal Isolation Sponsor");
    data.put("sponsorNameEn", "Portal Isolation Sponsor");
    data.put("sponsorAddressZh", "No. 27 Sponsor Road");
    data.put("sponsorAddressEn", "No. 27 Sponsor Road");
    data.put("manufacturerNameZh", "Portal Isolation Manufacturer");
    data.put("manufacturerNameEn", "Portal Isolation Manufacturer");
    data.put("manufacturerAddressZh", "No. 27 Manufacturer Road");
    data.put("manufacturerAddressEn", "No. 27 Manufacturer Road");
    data.put("contactName", "Portal Isolation Contact");
    data.put("contactPhone", "13800000027");
    data.put("contactEmail", "portal-isolation@example.com");
    data.put("officialReportEmail", "reports.portal-isolation@example.com");
    data.put("shippingInfo", "Ship to portal isolation sponsor");
    data.put("requiresGlp", false);
    data.put("requiresHardCopyOfElectronicReport", false);
    data.put("protocolLanguage", "CHS");
    data.put("reportLanguage", "CHS");
    data.put("submittedQuantityText", "10 pcs");
    data.put("sampleDisposition", "DESTROY_DISCARD");
    data.put("sampleNameZh", "Portal Isolation Sample");
    data.put("sampleNameEn", "Portal Isolation Sample");
    data.put("sterilizationState", "NON_STERILE");
    data.put("casCode", "N/A");
    data.put("modelRef", "MODEL-27");
    data.put("sizeSpec", "10 mm");
    data.put("lotBatch", "LOT-27");
    data.put("rawMaterial", "Polymer");
    data.put("packagingMaterial", "Pouch");
    data.put("physicalState", "SOLID");
    data.put("color", "White");
    data.put("density", "N/A");
    data.put("stability", "Stable");
    data.put("solubility", "Insoluble");
    data.put("storageCondition", "ROOM_TEMPERATURE");
    data.put("intendedUse", "Portal isolation validation");
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

  private String login(String username, String password) throws Exception {
    String response = mockMvc.perform(post("/auth/portal/login")
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

  private record CrossCustomerFixture(
      UUID submissionId,
      UUID projectId,
      UUID contractId,
      UUID reportId
  ) {
  }
}
