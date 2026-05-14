package com.njxs.lims.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.njxs.lims.rule.service.RuleEngineService;
import java.util.Map;
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
class RuleEngineServiceTests {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final RuleEngineService ruleEngineService;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  RuleEngineServiceTests(RuleEngineService ruleEngineService, JdbcTemplate jdbcTemplate) {
    this.ruleEngineService = ruleEngineService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void reloadsBusinessRuleAfterInvalidation() {
    assertThat(ruleEngineService.evaluate("PLAN_REQUIRES_QA", Map.of("requiresGlp", true))).isTrue();
    assertThat(ruleEngineService.evaluate("PLAN_REQUIRES_QA", Map.of("requiresGlp", false))).isFalse();

    jdbcTemplate.update(
        """
        update business_rule
        set condition_expr = 'false', updated_at = now()
        where rule_code = 'PLAN_REQUIRES_QA'
        """
    );
    ruleEngineService.invalidate("PLAN_REQUIRES_QA");

    assertThat(ruleEngineService.evaluate("PLAN_REQUIRES_QA", Map.of("requiresGlp", true))).isFalse();
  }
}
