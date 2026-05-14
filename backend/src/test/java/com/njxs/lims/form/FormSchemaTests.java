package com.njxs.lims.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class FormSchemaTests {

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
  FormSchemaTests(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  @Test
  void listsPublishedSubmissionVer15SchemaGeneratedFromCsv() throws Exception {
    String response = mockMvc.perform(get("/form-schemas")
            .queryParam("formCode", "SUBMISSION_VER15")
            .queryParam("status", "PUBLISHED"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    assertThat(root).hasSize(1);
    JsonNode formSchema = root.get(0);
    JsonNode schemaJson = formSchema.get("schemaJson");

    assertThat(formSchema.get("status").asText()).isEqualTo("PUBLISHED");
    assertThat(formSchema.get("formCode").asText()).isEqualTo("SUBMISSION_VER15");
    assertThat(formSchema.get("versionNo").asInt()).isEqualTo(1);
    assertThat(schemaJson.get("fields")).hasSize(csvFieldRows());
    assertThat(schemaJson.at("/properties/lotBatch/x-lock-policy").asText()).isEqualTo("HARD_LOCK");
    assertThat(schemaJson.at("/properties/sterilizationState/x-dictionary").asText())
        .isEqualTo("STERILIZATION_STATE");
    assertThat(StreamSupport.stream(schemaJson.get("required").spliterator(), false)
            .map(JsonNode::asText)
            .toList())
        .doesNotContain("sampleDispositionRemark")
        .contains("officialReportEmail");
  }

  private int csvFieldRows() throws IOException {
    Path repoRoot = Path.of(System.getProperty("user.dir")).getParent();
    Path path = repoRoot.resolve("docs")
        .resolve("development")
        .resolve("specs")
        .resolve("submission_ver15_fields.csv");
    try (var lines = Files.lines(path)) {
      return (int) lines.skip(1).count();
    }
  }
}
