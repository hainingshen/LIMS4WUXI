package com.njxs.lims.report.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.report.snapshot.service.ReportSnapshotService;
import java.util.Map;
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
class ReportSnapshotServiceTests {

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

  private final ReportSnapshotService reportSnapshotService;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  ReportSnapshotServiceTests(ReportSnapshotService reportSnapshotService, JdbcTemplate jdbcTemplate) {
    this.reportSnapshotService = reportSnapshotService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void sterilityMappingReportsMissingTargetFieldAndBuildsSnapshotAfterDataIsCompleted() {
    UUID projectId = insertProject();
    UUID sampleId = insertSample(projectId);
    UUID planId = insertStudyPlan(projectId);
    UUID workOrderId = insertWorkOrder(planId, projectId, sampleId);
    UUID rawRecordId = insertRawRecord(workOrderId, projectId, sampleId, rawRecordDataWithoutConclusion());
    UUID reportId = insertReport(projectId);
    insertReportWorkOrder(reportId, workOrderId, rawRecordId);

    assertThatThrownBy(() -> reportSnapshotService.buildSnapshot(reportId, "STERILITY_REPORT"))
        .isInstanceOf(BusinessException.class)
        .satisfies(error -> assertThat(((BusinessException) error).getDetails())
            .containsEntry("targetField", "conclusion")
            .containsEntry("reportLabel", "检测结论"));

    jdbcTemplate.update(
        "update raw_record set record_data = ?::jsonb where id = ?",
        rawRecordDataWithConclusion(),
        rawRecordId
    );

    Map<String, Object> snapshot = reportSnapshotService.buildSnapshot(reportId, "STERILITY_REPORT");
    assertThat(snapshot)
        .containsEntry("reportNo", "RPT-TODO61")
        .containsEntry("entrustingUnit", "Demo Sponsor")
        .containsEntry("sampleName", "Todo 61 Sample")
        .containsEntry("sampleNo", "SAMPLE-TODO61")
        .containsEntry("sumOfSample", "10pcs")
        .containsEntry("testItems", "STERILITY")
        .containsEntry("testBasis", "ISO 11737")
        .containsEntry("testEquipment", "Incubator(EQ-1)")
        .containsEntry("conclusion", "无菌")
        .containsEntry("watermark", "DRAFT")
        .containsEntry("seal", "NONE");
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
        ) values (?, 'P-TODO61', '00000000-0000-0000-0000-000000000401',
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
        ) values (?, 'SAMPLE-TODO61', ?, '00000000-0000-0000-0000-000000000401',
          'Todo 61 Sample', 'Todo 61 Sample', 'NON_STERILE', 'N/A', 'MODEL-61',
          '10 mm', 'LOT-61', 'Polymer', 'Pouch', 'SOLID', 'White', 'N/A',
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
        ) values (?, 'PLAN-TODO61', ?, 'COMPLETED', false, false, false,
          now(), now(), now(), 0, 'NEW', 100)
        """,
        planId,
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
          progress_percent, last_progress_at, created_at, updated_at, version,
          source_type, data_quality_score
        ) values (?, 'WO-TODO61', ?, ?, ?, ?, 'COMPLETED', 100,
          now(), now(), now(), 0, 'NEW', 100)
        """,
        workOrderId,
        planId,
        projectId,
        sampleId,
        INSPECTOR_ID
    );
    return workOrderId;
  }

  private UUID insertRawRecord(UUID workOrderId, UUID projectId, UUID sampleId, String recordData) {
    UUID rawRecordId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into raw_record (
          id, biz_no, work_order_id, project_id, sample_id, template_code, test_item,
          test_basis, test_time, test_location, temperature_celsius, humidity_percent,
          method, record_data, attachment_file_ids, status, tester_id, checker_id,
          submitted_at, reviewed_at, created_at, created_by, updated_at, updated_by,
          version, source_type, data_quality_score
        ) values (?, 'RR-TODO61', ?, ?, ?, 'RAW_RECORD_STERILITY', 'STERILITY',
          'ISO 11737', '2026-05-14 - 2026-05-21', 'Lab A', '22', '45',
          'DIRECT_INOCULATION', ?::jsonb, '[]'::jsonb, 'APPROVED', ?, ?,
          now(), now(), now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        rawRecordId,
        workOrderId,
        projectId,
        sampleId,
        recordData,
        INSPECTOR_ID,
        QC_ID,
        INSPECTOR_ID,
        QC_ID
    );
    return rawRecordId;
  }

  private UUID insertReport(UUID projectId) {
    UUID reportId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into report (
          id, biz_no, project_id, template_code, template_version, status,
          current_version_no, is_official, watermark_type, payment_gate_status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, 'RPT-TODO61', ?, 'STERILITY_REPORT', '1.0', 'DRAFT',
          1, false, 'DRAFT', 'LOCKED', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        reportId,
        projectId,
        ADMIN_ID,
        ADMIN_ID
    );
    return reportId;
  }

  private void insertReportWorkOrder(UUID reportId, UUID workOrderId, UUID rawRecordId) {
    jdbcTemplate.update(
        """
        insert into report_work_order (report_id, work_order_id, raw_record_id)
        values (?, ?, ?)
        """,
        reportId,
        workOrderId,
        rawRecordId
    );
  }

  private String rawRecordDataWithoutConclusion() {
    return """
        {
          "equipmentUsages": [
            {"name": "Incubator", "no": "EQ-1"}
          ],
          "sterilityResults": [
            {"sampleName": "Todo 61 Sample"}
          ]
        }
        """;
  }

  private String rawRecordDataWithConclusion() {
    return """
        {
          "equipmentUsages": [
            {"name": "Incubator", "no": "EQ-1"}
          ],
          "sterilityResults": [
            {"sampleName": "Todo 61 Sample", "conclusion": "无菌"}
          ]
        }
        """;
  }
}
