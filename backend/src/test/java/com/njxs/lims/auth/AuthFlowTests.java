package com.njxs.lims.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.auth.api.RefreshTokenRequest;
import com.njxs.lims.common.web.TraceIdFilter;
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
class AuthFlowTests {

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
  AuthFlowTests(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  @Test
  void internalLoginAndMeReturnClientTypeRolesAndScopes() throws Exception {
    JsonNode token = login("/auth/internal/login", "internal.admin", "Internal123!");

    assertThat(token.get("clientType").asText()).isEqualTo("INTERNAL");
    assertThat(token.get("roles")).anyMatch(role -> role.asText().equals("SYSTEM_ADMIN"));

    mockMvc.perform(get("/auth/me")
            .header(HttpHeaders.AUTHORIZATION, bearer(token.get("accessToken").asText())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("internal.admin"))
        .andExpect(jsonPath("$.clientType").value("INTERNAL"))
        .andExpect(jsonPath("$.roleCodes").isArray())
        .andExpect(jsonPath("$.dataScopes").isArray());
  }

  @Test
  void portalLoginFixesPortalClientTypeInToken() throws Exception {
    JsonNode token = login("/auth/portal/login", "portal.admin", "Portal123!");

    assertThat(token.get("clientType").asText()).isEqualTo("PORTAL");
    assertThat(token.get("roles")).anyMatch(role -> role.asText().equals("CUSTOMER_ORG_ADMIN"));

    mockMvc.perform(get("/auth/me")
            .header(HttpHeaders.AUTHORIZATION, bearer(token.get("accessToken").asText())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("portal.admin"))
        .andExpect(jsonPath("$.clientType").value("PORTAL"))
        .andExpect(jsonPath("$.customerOrgId").value("00000000-0000-0000-0000-000000000301"));
  }

  @Test
  void logoutBlacklistsAccessAndRefreshTokens() throws Exception {
    JsonNode token = login("/auth/internal/login", "internal.admin", "Internal123!");
    String accessToken = token.get("accessToken").asText();
    String refreshToken = token.get("refreshToken").asText();

    mockMvc.perform(post("/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/auth/me")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .header(TraceIdFilter.TRACE_ID_HEADER, "trace-auth-blacklist"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.traceId").value("trace-auth-blacklist"));

    mockMvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  private JsonNode login(String path, String username, String password) throws Exception {
    String body = """
        {"username":"%s","password":"%s"}
        """.formatted(username, password);
    String response = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
