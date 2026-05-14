package com.njxs.lims.project.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.project.domain.Project;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcProjectRepository implements ProjectRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcProjectRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextProjectBizNo(int year) {
    String prefix = "P-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from project where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    int sequence = (count == null ? 0 : count) + 1;
    return prefix + "%04d".formatted(sequence);
  }

  @Override
  public void save(Project project) {
    prepareForInsert(project);
    jdbcTemplate.update(
        """
        insert into project (
          id, biz_no, submission_id, customer_org_id, project_type, qualification_type, service_type,
          urgent_extra_fee_percent, urgent_tat_days, requires_glp, requires_animal_test,
          requires_ethics_review, requires_nonstandard_review, business_owner_id, technical_owner_id,
          sd_user_id, status, planned_finish_at, actual_finish_at, health_status, health_score,
          payment_status, created_at, created_by, updated_at, updated_by, deleted_at, version,
          tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        project.getId(),
        project.getBizNo(),
        project.getSubmissionId(),
        project.getCustomerOrgId(),
        project.getProjectType(),
        project.getQualificationType(),
        project.getServiceType(),
        project.getUrgentExtraFeePercent(),
        project.getUrgentTatDays(),
        project.isRequiresGlp(),
        project.isRequiresAnimalTest(),
        project.isRequiresEthicsReview(),
        project.isRequiresNonstandardReview(),
        project.getBusinessOwnerId(),
        project.getTechnicalOwnerId(),
        project.getSdUserId(),
        project.getStatus(),
        timestamp(project.getPlannedFinishAt()),
        timestamp(project.getActualFinishAt()),
        project.getHealthStatus(),
        project.getHealthScore(),
        project.getPaymentStatus(),
        timestamp(project.getCreatedAt()),
        project.getCreatedBy(),
        timestamp(project.getUpdatedAt()),
        project.getUpdatedBy(),
        timestamp(project.getDeletedAt()),
        project.getVersion(),
        project.getTenantOrgId(),
        project.getLegacyId(),
        project.getSourceType(),
        project.getMigrationBatch(),
        project.getDataQualityScore()
    );
  }
}
