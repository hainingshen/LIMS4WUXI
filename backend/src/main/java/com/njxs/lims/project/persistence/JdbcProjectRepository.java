package com.njxs.lims.project.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.permission.InternalDataScope;
import com.njxs.lims.project.domain.Project;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcProjectRepository implements ProjectRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, submission_id, customer_org_id, project_type, qualification_type, service_type,
      urgent_extra_fee_percent, urgent_tat_days, requires_glp, requires_animal_test,
      requires_ethics_review, requires_nonstandard_review, business_owner_id, technical_owner_id,
      sd_user_id, status, planned_finish_at, actual_finish_at, health_status, health_score,
      payment_status, created_at, created_by, updated_at, updated_by, deleted_at, version,
      tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
      """;
  private static final String SELECT_PROJECT_COLUMNS = "p."
      + SELECT_COLUMNS.strip().replaceAll("\\s+", " ").replace(", ", ", p.");

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

  @Override
  public Optional<Project> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from project
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> project(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<Project> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from project
        where deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> project(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from project
        where deleted_at is null
        """,
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<Project> listVisibleTo(InternalDataScope scope, int limit, int offset) {
    List<Object> args = new ArrayList<>();
    String predicate = visibleProjectPredicate(scope, args, "p");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(
        "select " + SELECT_PROJECT_COLUMNS + " " + """
        from project p
        where p.deleted_at is null
          and (%s)
        order by p.updated_at desc, p.created_at desc
        limit ? offset ?
        """.formatted(predicate),
        (rs, rowNum) -> project(rs),
        args.toArray()
    );
  }

  @Override
  public int countVisibleTo(InternalDataScope scope) {
    List<Object> args = new ArrayList<>();
    String predicate = visibleProjectPredicate(scope, args, "p");
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from project p
        where p.deleted_at is null
          and (%s)
        """.formatted(predicate),
        Integer.class,
        args.toArray()
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<Project> listForCustomerOrg(UUID customerOrgId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from project
        where customer_org_id = ?
          and deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> project(rs),
        customerOrgId,
        limit,
        offset
    );
  }

  @Override
  public int countForCustomerOrg(UUID customerOrgId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from project
        where customer_org_id = ?
          and deleted_at is null
        """,
        Integer.class,
        customerOrgId
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<Project> listForApplicant(UUID applicantUserId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_PROJECT_COLUMNS + " " + """
        from project p
        join submission s on s.id = p.submission_id
        join app_user u on u.id = s.applicant_user_id
        where s.applicant_user_id = ?
          and p.customer_org_id = u.customer_org_id
          and s.customer_org_id = u.customer_org_id
          and u.client_type = 'PORTAL'
          and u.customer_org_id is not null
          and p.deleted_at is null
          and s.deleted_at is null
        order by p.updated_at desc, p.created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> project(rs),
        applicantUserId,
        limit,
        offset
    );
  }

  @Override
  public int countForApplicant(UUID applicantUserId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from project p
        join submission s on s.id = p.submission_id
        join app_user u on u.id = s.applicant_user_id
        where s.applicant_user_id = ?
          and p.customer_org_id = u.customer_org_id
          and s.customer_org_id = u.customer_org_id
          and u.client_type = 'PORTAL'
          and u.customer_org_id is not null
          and p.deleted_at is null
          and s.deleted_at is null
        """,
        Integer.class,
        applicantUserId
    );
    return count == null ? 0 : count;
  }

  @Override
  public void updateStatus(Project project, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update project
        set status = ?, updated_at = now(), updated_by = ?, version = version + 1
        where id = ? and deleted_at is null
        """,
        project.getStatus(),
        actorUserId,
        project.getId()
    );
  }

  @Override
  public boolean existsByAssignedSales(UUID projectId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from project p
        join submission s on s.id = p.submission_id
        where p.id = ? and s.assigned_sales_user_id = ? and p.deleted_at is null
        """,
        Integer.class,
        projectId,
        userId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsByTechnicalOwner(UUID projectId, UUID userId) {
    return existsByOwnerColumn("technical_owner_id", projectId, userId);
  }

  @Override
  public boolean existsByBusinessOwner(UUID projectId, UUID userId) {
    return existsByOwnerColumn("business_owner_id", projectId, userId);
  }

  @Override
  public Optional<UUID> findFirstActiveUserIdByRole(String roleCode) {
    return jdbcTemplate.query(
        """
        select u.id
        from app_user u
        join user_role ur on ur.user_id = u.id
        where ur.role_code = ? and u.status = 'ACTIVE'
        order by u.created_at, u.id
        limit 1
        """,
        (rs, rowNum) -> rs.getObject("id", UUID.class),
        roleCode
    ).stream().findFirst();
  }

  private boolean existsByOwnerColumn(String columnName, UUID projectId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from project
        where id = ? and %s = ? and deleted_at is null
        """.formatted(columnName),
        Integer.class,
        projectId,
        userId
    );
    return count != null && count > 0;
  }

  private String visibleProjectPredicate(InternalDataScope scope, List<Object> args, String alias) {
    if (scope.allAccess()) {
      return "true";
    }
    List<String> clauses = new ArrayList<>();
    if (scope.hasRole("SALES_REP")) {
      clauses.add("""
          exists (
            select 1
            from submission sub
            where sub.id = %s.submission_id
              and sub.assigned_sales_user_id = ?
              and sub.deleted_at is null
          )
          """.formatted(alias));
      args.add(scope.userId());
    }
    if (scope.hasRole("BUSINESS_OWNER")) {
      clauses.add(alias + ".business_owner_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("TECHNICAL_LEADER")) {
      clauses.add(alias + ".technical_owner_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("SD")) {
      clauses.add(alias + ".sd_user_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("INSPECTOR")) {
      clauses.add("""
          exists (
            select 1
            from work_order wo
            where wo.project_id = %s.id
              and wo.owner_user_id = ?
              and wo.deleted_at is null
          )
          """.formatted(alias));
      args.add(scope.userId());
    }
    if (scope.hasRole("QC")) {
      clauses.add("""
          exists (
            select 1
            from raw_record rr
            where rr.project_id = %s.id
              and (rr.status = 'REVIEWING' or rr.checker_id = ?)
              and rr.deleted_at is null
          )
          """.formatted(alias));
      args.add(scope.userId());
    }
    if (scope.hasRole("REPORT_PREPARER")) {
      clauses.add("""
          exists (
            select 1
            from report r
            where r.project_id = %s.id
              and r.created_by = ?
              and r.deleted_at is null
          )
          """.formatted(alias));
      args.add(scope.userId());
    }
    if (scope.hasRole("AUTHORIZED_SIGNATORY")) {
      clauses.add("""
          exists (
            select 1
            from report r
            where r.project_id = %s.id
              and r.status = 'PENDING_ISSUE'
              and r.deleted_at is null
          )
          """.formatted(alias));
    }
    if (scope.hasRole("ARCHIVE_MANAGER")) {
      clauses.add("""
          exists (
            select 1
            from archive_item ai
            where ai.project_id = %s.id
              and ai.deleted_at is null
          )
          """.formatted(alias));
    }
    if (scope.hasAnyRole("FINANCE_CASHIER", "FINANCE_MANAGER")) {
      clauses.add("""
          (%s.payment_status <> 'PAID'
            or exists (
              select 1
              from payment_receipt pr
              where pr.project_id = %s.id
                and pr.deleted_at is null
            )
          )
          """.formatted(alias, alias));
    }
    if (clauses.isEmpty()) {
      return "false";
    }
    return String.join(" or ", clauses);
  }

  private Project project(ResultSet rs) throws SQLException {
    Project project = new Project();
    project.setId(rs.getObject("id", UUID.class));
    project.setBizNo(rs.getString("biz_no"));
    project.setSubmissionId(rs.getObject("submission_id", UUID.class));
    project.setCustomerOrgId(rs.getObject("customer_org_id", UUID.class));
    project.setProjectType(rs.getString("project_type"));
    project.setQualificationType(rs.getString("qualification_type"));
    project.setServiceType(rs.getString("service_type"));
    project.setUrgentExtraFeePercent(rs.getBigDecimal("urgent_extra_fee_percent"));
    project.setUrgentTatDays((Integer) rs.getObject("urgent_tat_days"));
    project.setRequiresGlp(rs.getBoolean("requires_glp"));
    project.setRequiresAnimalTest(rs.getBoolean("requires_animal_test"));
    project.setRequiresEthicsReview(rs.getBoolean("requires_ethics_review"));
    project.setRequiresNonstandardReview(rs.getBoolean("requires_nonstandard_review"));
    project.setBusinessOwnerId(rs.getObject("business_owner_id", UUID.class));
    project.setTechnicalOwnerId(rs.getObject("technical_owner_id", UUID.class));
    project.setSdUserId(rs.getObject("sd_user_id", UUID.class));
    project.setStatus(rs.getString("status"));
    project.setPlannedFinishAt(instant(rs.getTimestamp("planned_finish_at")));
    project.setActualFinishAt(instant(rs.getTimestamp("actual_finish_at")));
    project.setHealthStatus(rs.getString("health_status"));
    project.setHealthScore(rs.getInt("health_score"));
    project.setPaymentStatus(rs.getString("payment_status"));
    project.setCreatedAt(instant(rs.getTimestamp("created_at")));
    project.setCreatedBy(rs.getObject("created_by", UUID.class));
    project.setUpdatedAt(instant(rs.getTimestamp("updated_at")));
    project.setUpdatedBy(rs.getObject("updated_by", UUID.class));
    project.setDeletedAt(instant(rs.getTimestamp("deleted_at")));
    project.setVersion((Integer) rs.getObject("version"));
    project.setTenantOrgId(rs.getObject("tenant_org_id", UUID.class));
    project.setLegacyId(rs.getString("legacy_id"));
    project.setSourceType(rs.getString("source_type"));
    project.setMigrationBatch(rs.getString("migration_batch"));
    project.setDataQualityScore((Integer) rs.getObject("data_quality_score"));
    return project;
  }

  private Instant instant(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant();
  }
}
