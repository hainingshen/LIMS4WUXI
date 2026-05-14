package com.njxs.lims.submission.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.njxs.lims.common.domain.BaseEntity;
import com.njxs.lims.customer.domain.CustomerContact;
import com.njxs.lims.customer.domain.CustomerOrg;
import com.njxs.lims.customer.domain.Manufacturer;
import com.njxs.lims.customer.persistence.CustomerRepository;
import com.njxs.lims.sample.domain.Sample;
import com.njxs.lims.sample.persistence.SampleRepository;
import com.njxs.lims.submission.domain.Submission;
import com.njxs.lims.submission.domain.SubmissionTestSelection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SubmissionBaseRepositoryTests {

  private static final UUID APPLICANT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
  private static final UUID SALES_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
  private static final UUID CUSTOMER_ORG_ID = UUID.fromString("10000000-0000-0000-0000-000000000201");
  private static final UUID CUSTOMER_CONTACT_ID = UUID.fromString("10000000-0000-0000-0000-000000000202");
  private static final UUID MANUFACTURER_ID = UUID.fromString("10000000-0000-0000-0000-000000000203");
  private static final UUID SUBMISSION_ID = UUID.fromString("10000000-0000-0000-0000-000000000204");
  private static final UUID SELECTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000205");
  private static final UUID SAMPLE_ID = UUID.fromString("10000000-0000-0000-0000-000000000206");
  private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000207");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final CustomerRepository customerRepository;
  private final SubmissionBaseRepository submissionBaseRepository;
  private final SampleRepository sampleRepository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  SubmissionBaseRepositoryTests(
      CustomerRepository customerRepository,
      SubmissionBaseRepository submissionBaseRepository,
      SampleRepository sampleRepository,
      JdbcTemplate jdbcTemplate
  ) {
    this.customerRepository = customerRepository;
    this.submissionBaseRepository = submissionBaseRepository;
    this.sampleRepository = sampleRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void savesSubmissionWithTestSelectionAndSampleUsingSnakeCaseColumns() {
    customerRepository.saveCustomerOrg(customerOrg());
    customerRepository.saveCustomerContact(customerContact());
    customerRepository.saveManufacturer(manufacturer());
    submissionBaseRepository.saveSubmission(submission());
    submissionBaseRepository.saveTestSelection(selection());
    sampleRepository.saveSample(sample());

    assertThat(count("customer_org", CUSTOMER_ORG_ID)).isEqualTo(1);
    assertThat(count("customer_contact", CUSTOMER_CONTACT_ID)).isEqualTo(1);
    assertThat(count("manufacturer", MANUFACTURER_ID)).isEqualTo(1);
    assertThat(count("submission", SUBMISSION_ID)).isEqualTo(1);
    assertThat(count("submission_test_selection", SELECTION_ID)).isEqualTo(1);
    assertThat(count("sample", SAMPLE_ID)).isEqualTo(1);

    assertThat(columns("submission"))
        .contains(
            "customer_org_id",
            "customer_contact_id",
            "manufacturer_id",
            "official_report_email",
            "requires_hard_copy_of_electronic_report",
            "submitted_quantity_text",
            "sample_disposition"
        );
    assertThat(columns("submission_test_selection"))
        .contains("submission_id", "test_code", "method_name");
    assertThat(columns("sample"))
        .contains("sample_name_zh", "sample_name_en", "lot_batch", "storage_condition");
  }

  private CustomerOrg customerOrg() {
    CustomerOrg org = new CustomerOrg();
    base(org, CUSTOMER_ORG_ID, "CUST-T20-001");
    org.setNameZh("Todo 20 Sponsor");
    org.setNameEn("Todo 20 Sponsor");
    org.setAddressZh("Wuxi address");
    org.setAddressEn("Wuxi address");
    org.setStatus("ACTIVE");
    return org;
  }

  private CustomerContact customerContact() {
    CustomerContact contact = new CustomerContact();
    base(contact, CUSTOMER_CONTACT_ID, "CONTACT-T20-001");
    contact.setCustomerOrgId(CUSTOMER_ORG_ID);
    contact.setContactName("Todo Contact");
    contact.setPhone("13800000001");
    contact.setEmail("todo20@example.com");
    contact.setPrimary(true);
    contact.setStatus("ACTIVE");
    return contact;
  }

  private Manufacturer manufacturer() {
    Manufacturer manufacturer = new Manufacturer();
    base(manufacturer, MANUFACTURER_ID, "MFR-T20-001");
    manufacturer.setNameZh("Todo 20 Manufacturer");
    manufacturer.setNameEn("Todo 20 Manufacturer");
    manufacturer.setAddressZh("Manufacturer address");
    manufacturer.setAddressEn("Manufacturer address");
    manufacturer.setStatus("ACTIVE");
    return manufacturer;
  }

  private Submission submission() {
    Submission submission = new Submission();
    base(submission, SUBMISSION_ID, "SUB-2026-0020");
    submission.setCustomerOrgId(CUSTOMER_ORG_ID);
    submission.setCustomerContactId(CUSTOMER_CONTACT_ID);
    submission.setManufacturerId(MANUFACTURER_ID);
    submission.setApplicantUserId(APPLICANT_USER_ID);
    submission.setAssignedSalesUserId(SALES_USER_ID);
    submission.setStatus("DRAFT");
    submission.setOfficialReportEmail("reports.todo20@example.com");
    submission.setShippingInfo("Ship reports to Todo 20 sponsor");
    submission.setRequiresGlp(false);
    submission.setRequiresHardCopyOfElectronicReport(false);
    submission.setReportLanguage("CHS");
    submission.setProtocolLanguage("CHS");
    submission.setSubmittedQuantityText("10 pcs");
    submission.setSampleDisposition("DESTROY_DISCARD");
    return submission;
  }

  private SubmissionTestSelection selection() {
    SubmissionTestSelection selection = new SubmissionTestSelection();
    base(selection, SELECTION_ID, null);
    selection.setSubmissionId(SUBMISSION_ID);
    selection.setCategory("MICROBIOLOGY");
    selection.setTestCode("MICRO_STERILITY");
    selection.setTestName("Sterility test");
    selection.setMethodName("Membrane filtration");
    selection.setStandard("Chinese pharmacopoeia");
    return selection;
  }

  private Sample sample() {
    Sample sample = new Sample();
    base(sample, SAMPLE_ID, "S-20260514-020");
    sample.setProjectId(PROJECT_ID);
    sample.setSubmissionId(SUBMISSION_ID);
    sample.setSampleNameZh("Todo 20 Sample");
    sample.setSampleNameEn("Todo 20 Sample");
    sample.setSterilizationState("NON_STERILE");
    sample.setCasCode("N/A");
    sample.setModelRef("MODEL-20");
    sample.setSizeSpec("10 mm");
    sample.setLotBatch("LOT-20");
    sample.setRawMaterial("Polymer");
    sample.setPackagingMaterial("Pouch");
    sample.setPhysicalState("SOLID");
    sample.setColor("White");
    sample.setDensity("N/A");
    sample.setStability("Stable under room temperature");
    sample.setSolubility("Insoluble in water");
    sample.setStorageCondition("ROOM_TEMPERATURE");
    sample.setIntendedUse("Validation sample");
    sample.setAdditionalInfo("No additional info");
    sample.setQuantity(new BigDecimal("10.000"));
    sample.setQuantityUnit("pcs");
    sample.setStatus("WAITING_DELIVERY");
    return sample;
  }

  private void base(BaseEntity entity, UUID id, String bizNo) {
    entity.setId(id);
    entity.setBizNo(bizNo);
    entity.setCreatedBy(APPLICANT_USER_ID);
    entity.setUpdatedBy(APPLICANT_USER_ID);
    entity.setTenantOrgId(CUSTOMER_ORG_ID);
    entity.setSourceType("NEW");
    entity.setDataQualityScore(100);
  }

  private Integer count(String tableName, UUID id) {
    return jdbcTemplate.queryForObject(
        "select count(*) from " + tableName + " where id = ?",
        Integer.class,
        id
    );
  }

  private List<String> columns(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select column_name
        from information_schema.columns
        where table_schema = 'public'
          and table_name = ?
        """,
        String.class,
        tableName
    );
  }
}
