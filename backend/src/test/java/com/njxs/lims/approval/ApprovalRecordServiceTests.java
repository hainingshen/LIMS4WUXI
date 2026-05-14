package com.njxs.lims.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.njxs.lims.approval.domain.ApprovalRecord;
import com.njxs.lims.approval.service.ApprovalRecordService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApprovalRecordServiceTests {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private final ApprovalRecordService approvalRecordService;

  @Autowired
  ApprovalRecordServiceTests(ApprovalRecordService approvalRecordService) {
    this.approvalRecordService = approvalRecordService;
  }

  @Test
  void recordsTechnicalAndBusinessApprovalsForSameProject() {
    UUID projectId = UUID.randomUUID();
    UUID technicalApprover = UUID.randomUUID();
    UUID businessApprover = UUID.randomUUID();

    approvalRecordService.record("project", projectId, "TECH", "APPROVED", "技术通过", technicalApprover);
    approvalRecordService.record("project", projectId, "BUSINESS", "APPROVED", "业务通过", businessApprover);

    assertThat(approvalRecordService.listByEntity("project", projectId))
        .extracting(ApprovalRecord::stage)
        .containsExactlyInAnyOrder("TECH", "BUSINESS");
    assertThat(approvalRecordService.hasApproved("project", projectId, "TECH")).isTrue();
    assertThat(approvalRecordService.hasApproved("project", projectId, "BUSINESS")).isTrue();
  }
}
