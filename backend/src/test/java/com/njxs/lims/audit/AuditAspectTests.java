package com.njxs.lims.audit;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
class AuditAspectTests {

  private static final UUID ACCEPT_SUCCESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
  private static final UUID ACCEPT_FAILURE_ID = UUID.fromString("00000000-0000-0000-0000-000000000502");

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
  AuditAspectTests(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void auditedAcceptWritesSuccessLog() throws Exception {
    String accessToken = loginInternalSales();

    mockMvc.perform(post("/submissions/{id}/accept", ACCEPT_SUCCESS_ID)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"));

    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from audit_log
        where action_code = 'SUBMISSION_ACCEPT'
          and entity_id = ?
          and actor_user_id = '00000000-0000-0000-0000-000000000102'
          and actor_role_code = 'SALES_REP'
          and result = 'SUCCESS'
        """,
        Integer.class,
        ACCEPT_SUCCESS_ID
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  void auditedAcceptWritesFailureLog() throws Exception {
    String accessToken = loginInternalSales();

    mockMvc.perform(post("/submissions/{id}/accept", ACCEPT_FAILURE_ID)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SUBMISSION_STATUS_CONFLICT"));

    String failureReason = jdbcTemplate.queryForObject(
        """
        select failure_reason
        from audit_log
        where action_code = 'SUBMISSION_ACCEPT'
          and entity_id = ?
          and result = 'FAILURE'
        order by occurred_at desc
        limit 1
        """,
        String.class,
        ACCEPT_FAILURE_ID
    );
    assertThat(failureReason).contains("SUBMISSION_STATUS_CONFLICT");
  }

  private String loginInternalSales() throws Exception {
    String response = mockMvc.perform(post("/auth/internal/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"internal.sales\",\"password\":\"Sales123!\"}"))
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
