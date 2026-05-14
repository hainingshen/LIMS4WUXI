package com.njxs.lims.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
class FlywayMigrationTests {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  FlywayMigrationTests(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void flywayCreatesBaseTablesAndLoadsSeeds() throws IOException {
    assertThat(tableCount()).isEqualTo(12);
    assertThat(count("role_definition")).isEqualTo(csvRows("roles.csv"));
    assertThat(count("permission_matrix")).isEqualTo(csvRows("permission_matrix.csv"));
    assertThat(count("dictionary_item")).isEqualTo(csvRows("dictionaries.csv"));
  }

  private Integer tableCount() {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from information_schema.tables
        where table_schema = 'public'
          and table_name in (
            'role_definition',
            'permission_matrix',
            'dictionary_item',
            'organization_unit',
            'app_user',
            'user_role',
            'system_config',
            'business_rule',
            'form_schema',
            'approval_record',
            'audit_log',
            'flyway_schema_history'
          )
        """,
        Integer.class
    );
  }

  private Integer count(String tableName) {
    return jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
  }

  private int csvRows(String name) throws IOException {
    Path repoRoot = Path.of(System.getProperty("user.dir")).getParent();
    Path path = repoRoot.resolve("docs").resolve("development").resolve("specs").resolve(name);
    try (var lines = Files.lines(path)) {
      return (int) lines.skip(1).count();
    }
  }
}
