package com.njxs.lims.study.persistence;

import com.njxs.lims.permission.InternalDataScope;
import com.njxs.lims.study.domain.StudyPlanRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcStudyPlanRepository implements StudyPlanRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, status, requires_conditioning, requires_glp, requires_animal_test,
      planned_start_at, planned_finish_at, sd_approved_at, qa_approved_at, ethics_approved_at
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcStudyPlanRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextPlanBizNo(int year) {
    String prefix = "PLAN-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from study_plan where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%04d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public String nextWorkOrderBizNo(int year) {
    String prefix = "WO-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from work_order where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public StudyPlanRecord save(StudyPlanRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into study_plan (
          id, biz_no, project_id, status, requires_conditioning, requires_glp, requires_animal_test,
          planned_start_at, planned_finish_at, sd_approved_at, qa_approved_at, ethics_approved_at,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, ?, ?, ?)
        """,
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.status(),
        record.requiresConditioning(),
        record.requiresGlp(),
        record.requiresAnimalTest(),
        timestamp(record.plannedStartAt()),
        timestamp(record.plannedFinishAt()),
        timestamp(record.sdApprovedAt()),
        timestamp(record.qaApprovedAt()),
        timestamp(record.ethicsApprovedAt()),
        actorUserId,
        actorUserId,
        0,
        "NEW",
        100
    );
    return record;
  }

  @Override
  public Optional<StudyPlanRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + " from study_plan where id = ? and deleted_at is null",
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<StudyPlanRecord> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from study_plan
        where deleted_at is null
        order by created_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from study_plan where deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<StudyPlanRecord> listVisibleTo(InternalDataScope scope, int limit, int offset) {
    List<Object> args = new ArrayList<>();
    String predicate = visiblePlanPredicate(scope, args);
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(
        "select sp." + SELECT_COLUMNS.strip().replaceAll("\\s+", " ").replace(", ", ", sp.") + " " + """
        from study_plan sp
        join project p on p.id = sp.project_id and p.deleted_at is null
        left join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        where sp.deleted_at is null
          and (%s)
        order by sp.created_at desc, sp.biz_no desc
        limit ? offset ?
        """.formatted(predicate),
        (rs, rowNum) -> record(rs),
        args.toArray()
    );
  }

  @Override
  public int countVisibleTo(InternalDataScope scope) {
    List<Object> args = new ArrayList<>();
    String predicate = visiblePlanPredicate(scope, args);
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from study_plan sp
        join project p on p.id = sp.project_id and p.deleted_at is null
        left join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        where sp.deleted_at is null
          and (%s)
        """.formatted(predicate),
        Integer.class,
        args.toArray()
    );
    return count == null ? 0 : count;
  }

  @Override
  public StudyPlanRecord markReviewed(UUID id, String status, Instant reviewedAt, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update study_plan
        set status = ?,
            sd_approved_at = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        """,
        status,
        timestamp(reviewedAt),
        actorUserId,
        id
    );
    return findById(id).orElseThrow();
  }

  @Override
  public StudyPlanRecord updateStatus(UUID id, String status, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update study_plan
        set status = ?, updated_at = now(), updated_by = ?, version = version + 1
        where id = ? and deleted_at is null
        """,
        status,
        actorUserId,
        id
    );
    return findById(id).orElseThrow();
  }

  @Override
  public int createWorkOrders(UUID studyPlanId, UUID actorUserId) {
    StudyPlanRecord plan = findById(studyPlanId).orElseThrow();
    List<UUID> sampleIds = jdbcTemplate.query(
        """
        select id
        from sample
        where project_id = ? and deleted_at is null
        order by biz_no
        """,
        (rs, rowNum) -> rs.getObject("id", UUID.class),
        plan.projectId()
    );
    UUID ownerUserId = defaultInspectorUserId().orElseGet(() -> technicalOwnerId(plan.projectId()));
    int year = Instant.now().atZone(ZoneOffset.UTC).getYear();
    int created = 0;
    for (UUID sampleId : sampleIds) {
      jdbcTemplate.update(
          """
          insert into work_order (
            id, biz_no, study_plan_id, project_id, sample_id, owner_user_id, status,
            progress_percent, planned_finish_at, created_at, created_by, updated_at,
            updated_by, version, source_type, data_quality_score
          ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, ?, ?, ?)
          """,
          UUID.randomUUID(),
          nextWorkOrderBizNo(year),
          studyPlanId,
          plan.projectId(),
          sampleId,
          ownerUserId,
          "PENDING_EXECUTION",
          0,
          timestamp(plan.plannedFinishAt()),
          actorUserId,
          actorUserId,
          0,
          "NEW",
          100
      );
      created++;
    }
    return created;
  }

  private Optional<UUID> defaultInspectorUserId() {
    return jdbcTemplate.query(
        """
        select u.id
        from app_user u
        join user_role ur on ur.user_id = u.id
        where ur.role_code = 'INSPECTOR'
          and u.status = 'ACTIVE'
        order by u.created_at, u.id
        limit 1
        """,
        (rs, rowNum) -> rs.getObject("id", UUID.class)
    ).stream().findFirst();
  }

  private UUID technicalOwnerId(UUID projectId) {
    return jdbcTemplate.queryForObject(
        "select technical_owner_id from project where id = ?",
        UUID.class,
        projectId
    );
  }

  @Override
  public int workOrderCount(UUID studyPlanId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from work_order where study_plan_id = ? and deleted_at is null",
        Integer.class,
        studyPlanId
    );
    return count == null ? 0 : count;
  }

  @Override
  public boolean signedContractExists(UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract
        where project_id = ? and status = 'SIGNED' and deleted_at is null
        """,
        Integer.class,
        projectId
    );
    return count != null && count > 0;
  }

  private String visiblePlanPredicate(InternalDataScope scope, List<Object> args) {
    if (scope.allAccess()) {
      return "true";
    }
    List<String> clauses = new ArrayList<>();
    if (scope.hasRole("SALES_REP")) {
      clauses.add("sub.assigned_sales_user_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("BUSINESS_OWNER")) {
      clauses.add("p.business_owner_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("TECHNICAL_LEADER")) {
      clauses.add("p.technical_owner_id = ?");
      args.add(scope.userId());
    }
    if (scope.hasRole("SD")) {
      clauses.add("sp.status in ('REVIEWING', 'PENDING_DISPATCH', 'READY_TO_DISPATCH', 'DISPATCHED', 'COMPLETED')");
    }
    if (scope.hasRole("QA")) {
      clauses.add("sp.requires_glp = true");
    }
    if (scope.hasRole("ETHICS_COMMITTEE")) {
      clauses.add("sp.requires_animal_test = true");
    }
    if (clauses.isEmpty()) {
      return "false";
    }
    return String.join(" or ", clauses);
  }

  private StudyPlanRecord record(ResultSet rs) throws SQLException {
    return new StudyPlanRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("project_id", UUID.class),
        rs.getString("status"),
        rs.getBoolean("requires_conditioning"),
        rs.getBoolean("requires_glp"),
        rs.getBoolean("requires_animal_test"),
        instant(rs.getTimestamp("planned_start_at")),
        instant(rs.getTimestamp("planned_finish_at")),
        instant(rs.getTimestamp("sd_approved_at")),
        instant(rs.getTimestamp("qa_approved_at")),
        instant(rs.getTimestamp("ethics_approved_at"))
    );
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
