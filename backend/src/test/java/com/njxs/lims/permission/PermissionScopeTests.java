package com.njxs.lims.permission;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PermissionScopeTests {

  private static final UUID VIEWER_DENIED_SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID APPLICANT_DRAFT_SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000402");

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
  PermissionScopeTests(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  @Test
  void customerViewerCannotSubmitSubmission() throws Exception {
    String accessToken = login("portal.viewer", "Viewer123!");

    mockMvc.perform(post("/submissions/{id}/submit", VIEWER_DENIED_SUBMISSION_ID)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.details.actionCode").value("SUBMISSION_CREATE"));
  }

  @Test
  void customerApplicantCanSubmitOwnDraft() throws Exception {
    String accessToken = login("portal.applicant", "Applicant123!");

    mockMvc.perform(post("/submissions/{id}/submit", APPLICANT_DRAFT_SUBMISSION_ID)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(APPLICANT_DRAFT_SUBMISSION_ID.toString()))
        .andExpect(jsonPath("$.applicantUserId").value("00000000-0000-0000-0000-000000000202"))
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  private String login(String username, String password) throws Exception {
    String body = """
        {"username":"%s","password":"%s"}
        """.formatted(username, password);
    String response = mockMvc.perform(post("/auth/portal/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
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
