package com.njxs.lims.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class DeviceFileTests {

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
  DeviceFileTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void equipmentManagerMaintainsCurrentFileAndInspectorReadsOnlyAssignedDevice() throws Exception {
    String equipmentToken = login("internal", "internal.equipment_manager", "Sales123!");
    String inspectorToken = login("internal", "internal.inspector", "Sales123!");
    String otherInspectorToken = login("internal", "internal.inspector2", "Sales123!");
    UUID deviceId = insertDevice();
    UUID firstFileId = insertFile("todo76-calibration-v1.pdf", "application/pdf");
    UUID secondFileId = insertFile("todo76-calibration-v2.pdf", "application/pdf");
    linkAssignedWorkOrder(deviceId);

    mockMvc.perform(post("/devices/{id}/files", deviceId)
            .header(HttpHeaders.AUTHORIZATION, bearer(equipmentToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fileType": "CALIBRATION_CERT",
                  "fileId": "%s",
                  "versionNo": "v1",
                  "validFrom": "2026-05-01",
                  "validUntil": "2026-05-20",
                  "isCurrent": true
                }
                """.formatted(firstFileId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isCurrent").value(true))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    MvcResult secondUpload = mockMvc.perform(post("/devices/{id}/files", deviceId)
            .header(HttpHeaders.AUTHORIZATION, bearer(equipmentToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fileType": "CALIBRATION_CERT",
                  "fileId": "%s",
                  "versionNo": "v2",
                  "validFrom": "2026-05-21",
                  "validUntil": "2026-06-30",
                  "isCurrent": true
                }
                """.formatted(secondFileId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileId").value(secondFileId.toString()))
        .andExpect(jsonPath("$.isCurrent").value(true))
        .andReturn();
    UUID secondDeviceFileId = UUID.fromString(objectMapper.readTree(secondUpload.getResponse().getContentAsString())
        .get("id")
        .asText());

    mockMvc.perform(get("/devices/{id}/files", deviceId)
            .header(HttpHeaders.AUTHORIZATION, bearer(equipmentToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fileId").value(secondFileId.toString()))
        .andExpect(jsonPath("$[0].isCurrent").value(true))
        .andExpect(jsonPath("$[0].validUntil").value("2026-06-30"))
        .andExpect(jsonPath("$[1].fileId").value(firstFileId.toString()))
        .andExpect(jsonPath("$[1].isCurrent").value(false));

    mockMvc.perform(get("/devices/{id}/files", deviceId)
            .header(HttpHeaders.AUTHORIZATION, bearer(inspectorToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].deviceId").value(deviceId.toString()));

    mockMvc.perform(get("/devices/{id}/files", deviceId)
            .header(HttpHeaders.AUTHORIZATION, bearer(otherInspectorToken)))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/device-files/{id}/disable", secondDeviceFileId)
            .header(HttpHeaders.AUTHORIZATION, bearer(equipmentToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISABLED"))
        .andExpect(jsonPath("$.isCurrent").value(false));

    assertCurrentFileState(deviceId, secondDeviceFileId);
  }

  private UUID insertDevice() {
    UUID deviceId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into device (
          id, device_no, name, model, manufacturer, location, owner_user_id,
          metrology_required, status, created_at, created_by, updated_at, updated_by,
          version, source_type, data_quality_score
        ) values (?, ?, 'TODO 76 Incubator', 'M-76', 'NJXS', 'Lab A',
          ?, true, 'ACTIVE', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        deviceId,
        "DEV-TODO76-" + deviceId,
        INSPECTOR_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return deviceId;
  }

  private UUID insertFile(String fileName, String contentType) {
    UUID fileId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into file_metadata (
          id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, 1024, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        fileId,
        fileName,
        "devices/todo76/" + fileId + "/" + fileName,
        contentType,
        "sha256-" + fileId,
        ADMIN_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    return fileId;
  }

  private void linkAssignedWorkOrder(UUID deviceId) {
    UUID projectId = insertProject();
    UUID sampleId = insertSample(projectId);
    UUID planId = insertStudyPlan(projectId);
    UUID workOrderId = insertWorkOrder(planId, projectId, sampleId);
    UUID rawRecordId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into raw_record (
          id, biz_no, work_order_id, project_id, sample_id, template_code,
          test_item, test_basis, record_data, status, tester_id, submitted_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, 'STERILITY_RAW',
          'Sterility', 'GB/T TODO76', '{}'::jsonb, 'DRAFT', ?, now(),
          now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        rawRecordId,
        "RR-TODO76-" + rawRecordId,
        workOrderId,
        projectId,
        sampleId,
        INSPECTOR_ID,
        ADMIN_ID,
        ADMIN_ID
    );
    jdbcTemplate.update(
        """
        insert into equipment_usage (
          id, raw_record_id, device_id, device_name, device_no, model,
          calibration_valid_until, created_at, created_by, updated_at,
          updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, 'TODO 76 Incubator', 'DEV-TODO76', 'M-76',
          '2026-06-30', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        UUID.randomUUID(),
        rawRecordId,
        deviceId,
        ADMIN_ID,
        ADMIN_ID
    );
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
        "P-TODO76-" + projectId,
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
          'Todo 76 Sample', 'Todo 76 Sample', 'NON_STERILE', 'N/A', 'MODEL-76',
          '10 mm', 'LOT-76', 'Polymer', 'Pouch', 'SOLID', 'White', 'N/A',
          'Stable', 'Insoluble', 'ROOM_TEMPERATURE', 'Validation sample',
          'No additional info', 10, 'pcs', 'IN_TESTING',
          '2026-05-14T00:00:00Z', '2026-05-14', now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        sampleId,
        "SAMPLE-TODO76-" + sampleId,
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
        "PLAN-TODO76-" + planId,
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
        ) values (?, ?, ?, ?, ?, ?, 'IN_PROGRESS', 50, now(), now(), 0, 'NEW', 100)
        """,
        workOrderId,
        "WO-TODO76-" + workOrderId,
        planId,
        projectId,
        sampleId,
        INSPECTOR_ID
    );
    return workOrderId;
  }

  private void assertCurrentFileState(UUID deviceId, UUID disabledDeviceFileId) {
    Integer currentActive = jdbcTemplate.queryForObject(
        """
        select count(*)
        from device_file
        where device_id = ?
          and file_type = 'CALIBRATION_CERT'
          and is_current = true
          and status = 'ACTIVE'
        """,
        Integer.class,
        deviceId
    );
    String disabledStatus = jdbcTemplate.queryForObject(
        "select status from device_file where id = ?",
        String.class,
        disabledDeviceFileId
    );
    assertThat(currentActive).isZero();
    assertThat(disabledStatus).isEqualTo("DISABLED");
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
