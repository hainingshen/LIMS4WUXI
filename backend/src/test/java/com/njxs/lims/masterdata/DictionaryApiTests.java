package com.njxs.lims.masterdata;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class DictionaryApiTests {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final MockMvc mockMvc;

  @Autowired
  DictionaryApiTests(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void listsEnabledDictionaryItemsByCodeInSortOrder() throws Exception {
    mockMvc.perform(get("/dictionaries").queryParam("dictCode", "YES_NO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].dictCode").value("YES_NO"))
        .andExpect(jsonPath("$[0].itemCode").value("YES"))
        .andExpect(jsonPath("$[0].enabled").value(true))
        .andExpect(jsonPath("$[1].itemCode").value("NO"));
  }
}
