package com.njxs.lims.importexport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ImportExportTaskTests {

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
  ImportExportTaskTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void validatesFailedImportWithoutResultFileAndRunsSuccessfulExport() throws Exception {
    String qualityToken = login("internal", "internal.admin", "Internal123!");

    UUID failedImportId = createTask(qualityToken, """
        {
          "taskType": "MASTER_IMPORT",
          "entityType": "CUSTOMER_ORG",
          "parameters": {"batch": "todo77-invalid"}
        }
        """);
    mockMvc.perform(post("/import-export-tasks/{id}/validate", failedImportId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"))
        .andExpect(jsonPath("$.errorSummary").value("sourceFileId is required for MASTER_IMPORT"));
    assertFailedImportHasNoResultFile(failedImportId);

    UUID exportTaskId = createTask(qualityToken, """
        {
          "taskType": "LIST_EXPORT",
          "entityType": "PROJECT",
          "parameters": {"keyword": "todo77"}
        }
        """);
    mockMvc.perform(post("/import-export-tasks/{id}/validate", exportTaskId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.errorSummary").value("VALIDATION_PASSED"));

    mockMvc.perform(post("/import-export-tasks/{id}/run", exportTaskId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.resultFileId").isString())
        .andExpect(jsonPath("$.errorSummary").value("SUCCESS: LIST_EXPORT completed"));

    mockMvc.perform(get("/import-export-tasks/{id}/result-file", exportTaskId)
            .header(HttpHeaders.AUTHORIZATION, bearer(qualityToken)))
        .andExpect(status().isOk());
    assertSuccessfulExportRecordedResult(exportTaskId);
  }

  private UUID createTask(String token, String content) throws Exception {
    MvcResult created = mockMvc.perform(post("/import-export-tasks")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(content))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();
    JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
    return UUID.fromString(body.get("id").asText());
  }

  private void assertFailedImportHasNoResultFile(UUID taskId) {
    Map<String, Object> task = jdbcTemplate.queryForMap(
        """
        select status, result_file_id, error_summary
        from import_export_task
        where id = ?
        """,
        taskId
    );
    assertThat(task)
        .containsEntry("status", "FAILED")
        .containsEntry("error_summary", "sourceFileId is required for MASTER_IMPORT");
    assertThat(task.get("result_file_id")).isNull();
  }

  private void assertSuccessfulExportRecordedResult(UUID taskId) {
    Map<String, Object> task = jdbcTemplate.queryForMap(
        """
        select iet.status, iet.error_summary, iet.parameters::text parameters,
               fm.sha256, fm.content_type
        from import_export_task iet
        join file_metadata fm on fm.id = iet.result_file_id
        where iet.id = ?
        """,
        taskId
    );
    assertThat(task)
        .containsEntry("status", "SUCCESS")
        .containsEntry("error_summary", "SUCCESS: LIST_EXPORT completed")
        .containsEntry("sha256", "sha256-" + taskId)
        .containsEntry("content_type", "application/json");
    assertThat((String) task.get("parameters")).contains("todo77");
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
