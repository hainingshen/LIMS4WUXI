package com.njxs.lims.workcenter;

import static org.assertj.core.api.Assertions.assertThat;

import com.njxs.lims.workcenter.domain.ProjectHealth;
import com.njxs.lims.workcenter.service.ProjectHealthService;
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
class ProjectHealthServiceTests {

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

  private final ProjectHealthService projectHealthService;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  ProjectHealthServiceTests(ProjectHealthService projectHealthService, JdbcTemplate jdbcTemplate) {
    this.projectHealthService = projectHealthService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void staleDaysConfigurationChangesProjectHealthImmediately() {
    UUID projectId = insertProject();
    UUID sampleId = insertSample(projectId);
    UUID planId = insertStudyPlan(projectId);
    insertWorkOrder(planId, projectId, sampleId);

    ProjectHealth defaultHealth = projectHealthService.recalculate(projectId);

    assertThat(defaultHealth.score()).isEqualTo(100);
    assertThat(defaultHealth.status()).isEqualTo("NORMAL");

    jdbcTemplate.update(
        "update system_config set config_value = '1', updated_at = now() where config_key = 'WORK_CENTER_STALE_DAYS'"
    );

    ProjectHealth staleHealth = projectHealthService.recalculate(projectId);

    assertThat(staleHealth.score()).isEqualTo(80);
    assertThat(staleHealth.status()).isEqualTo("WARNING");
    assertProjectHealth(projectId, 80, "WARNING");
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
        "P-TODO80-" + projectId,
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
        ) values (?, ?, ?, '00000000-0000-0000-0000-000000000401',
          'Todo 80 Sample', 'Todo 80 Sample', 'NON_STERILE', 'N/A', 'MODEL-80',
          '10 mm', 'LOT-80', 'Polymer', 'Pouch', 'SOLID', 'White', 'N/A',
          'Stable', 'Insoluble', 'ROOM_TEMPERATURE', 'Validation sample',
          'No additional info', 10, 'pcs', 'IN_TESTING',
          '2026-05-14T00:00:00Z', '2026-05-14', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        sampleId,
        "SAMPLE-TODO80-" + sampleId,
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
        ) values (?, ?, ?, 'DISPATCHED', false, false, false, now(), now(), now(), 0, 'NEW', 100)
        """,
        planId,
        "PLAN-TODO80-" + planId,
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
          progress_percent, last_progress_at, planned_finish_at, created_at, updated_at,
          version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, 'IN_PROGRESS', 50,
          now() - interval '36 hours', now() + interval '3 days',
          now(), now(), 0, 'NEW', 100)
        """,
        workOrderId,
        "WO-TODO80-" + workOrderId,
        planId,
        projectId,
        sampleId,
        INSPECTOR_ID
    );
    return workOrderId;
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
}
