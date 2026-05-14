package com.njxs.lims.workcenter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njxs.lims.workcenter.domain.WorkCenterBlockerRecord;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcWorkCenterBlockerRepository implements WorkCenterBlockerRepository {

  private static final String SELECT_COLUMNS = """
      id, project_id, blocker_type, severity, description, impact_scope,
      expected_recovery_at, owner_user_id, collaborator_ids, status, resolution
      """;
  private static final String SELECT_COLUMNS_WITH_ALIAS = """
      wb.id, wb.project_id, wb.blocker_type, wb.severity, wb.description, wb.impact_scope,
      wb.expected_recovery_at, wb.owner_user_id, wb.collaborator_ids, wb.status, wb.resolution
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public JdbcWorkCenterBlockerRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public WorkCenterBlockerRecord insert(WorkCenterBlockerRecord record, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "insert into work_center_blocker " + """
        (
          id, project_id, blocker_type, severity, description, impact_scope,
          expected_recovery_at, owner_user_id, collaborator_ids, status,
          resolution, created_at, created_by, updated_at, updated_by, version,
          source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        record.id(),
        record.projectId(),
        record.blockerType(),
        record.severity(),
        record.description(),
        record.impactScope(),
        Timestamp.from(record.expectedRecoveryAt()),
        record.ownerUserId(),
        collaboratorIdsJson(record.collaboratorIds()),
        record.status(),
        record.resolution(),
        actorUserId,
        actorUserId
    );
  }

  @Override
  public List<WorkCenterBlockerRecord> list(WorkCenterScope scope, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS_WITH_ALIAS + """
        from work_center_blocker wb
        join project p on p.id = wb.project_id and p.deleted_at is null
        join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        where wb.deleted_at is null
          and (
            ? = true
            or p.business_owner_id = ?
            or p.technical_owner_id = ?
            or p.sd_user_id = ?
            or sub.assigned_sales_user_id = ?
            or exists (
              select 1
              from work_order wo
              where wo.project_id = p.id
                and wo.owner_user_id = ?
                and wo.deleted_at is null
            )
            or exists (
              select 1
              from raw_record rr
              where rr.project_id = p.id
                and (rr.tester_id = ? or rr.checker_id = ?)
                and rr.deleted_at is null
            )
          )
        order by
          case when wb.status = 'CLOSED' then 1 else 0 end,
          wb.expected_recovery_at asc,
          wb.created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        scope.allAccess(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        limit,
        offset
    );
  }

  @Override
  public int count(WorkCenterScope scope) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from work_center_blocker wb
        join project p on p.id = wb.project_id and p.deleted_at is null
        join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        where wb.deleted_at is null
          and (
            ? = true
            or p.business_owner_id = ?
            or p.technical_owner_id = ?
            or p.sd_user_id = ?
            or sub.assigned_sales_user_id = ?
            or exists (
              select 1
              from work_order wo
              where wo.project_id = p.id
                and wo.owner_user_id = ?
                and wo.deleted_at is null
            )
            or exists (
              select 1
              from raw_record rr
              where rr.project_id = p.id
                and (rr.tester_id = ? or rr.checker_id = ?)
                and rr.deleted_at is null
            )
          )
        """,
        Integer.class,
        scope.allAccess(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId()
    );
    return count == null ? 0 : count;
  }

  @Override
  public Optional<WorkCenterBlockerRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from work_center_blocker
        where id = ?
          and deleted_at is null
        """,
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public WorkCenterBlockerRecord update(WorkCenterBlockerRecord record, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update work_center_blocker " + """
        set blocker_type = ?,
            severity = ?,
            description = ?,
            impact_scope = ?,
            expected_recovery_at = ?,
            owner_user_id = ?,
            collaborator_ids = ?::jsonb,
            status = ?,
            resolution = ?,
            closed_at = case when ? = 'CLOSED' then now() else closed_at end,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> record(rs),
        record.blockerType(),
        record.severity(),
        record.description(),
        record.impactScope(),
        Timestamp.from(record.expectedRecoveryAt()),
        record.ownerUserId(),
        collaboratorIdsJson(record.collaboratorIds()),
        record.status(),
        record.resolution(),
        record.status(),
        actorUserId,
        record.id()
    );
  }

  @Override
  public boolean projectExists(UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from project where id = ? and deleted_at is null",
        Integer.class,
        projectId
    );
    return count != null && count > 0;
  }

  private WorkCenterBlockerRecord record(ResultSet rs) throws SQLException {
    return new WorkCenterBlockerRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("project_id", UUID.class),
        rs.getString("blocker_type"),
        rs.getString("severity"),
        rs.getString("description"),
        rs.getString("impact_scope"),
        instant(rs.getTimestamp("expected_recovery_at")),
        rs.getObject("owner_user_id", UUID.class),
        collaboratorIds(rs.getString("collaborator_ids")),
        rs.getString("status"),
        rs.getString("resolution")
    );
  }

  private String collaboratorIdsJson(List<UUID> collaboratorIds) {
    try {
      return objectMapper.writeValueAsString(collaboratorIds == null ? List.of() : collaboratorIds);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize blocker collaborators", exception);
    }
  }

  private List<UUID> collaboratorIds(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(value, new TypeReference<>() {
      });
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to deserialize blocker collaborators", exception);
    }
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
