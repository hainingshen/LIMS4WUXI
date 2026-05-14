package com.njxs.lims.study.persistence;

import com.njxs.lims.study.domain.WorkOrderRecord;
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
public class JdbcWorkOrderRepository implements WorkOrderRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, study_plan_id, project_id, sample_id, owner_user_id, status,
      progress_percent, planned_finish_at
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcWorkOrderRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<WorkOrderRecord> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + " from work_order where id = ? and deleted_at is null",
        (rs, rowNum) -> record(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<WorkOrderRecord> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from work_order
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
        "select count(*) from work_order where deleted_at is null",
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public WorkOrderRecord updateStatus(UUID id, String status, int progressPercent, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update work_order
        set status = ?,
            progress_percent = ?,
            last_progress_at = now(),
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        """,
        status,
        progressPercent,
        actorUserId,
        id
    );
    return findById(id).orElseThrow();
  }

  @Override
  public boolean existsAssigned(UUID id, UUID ownerUserId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from work_order
        where id = ? and owner_user_id = ? and deleted_at is null
        """,
        Integer.class,
        id,
        ownerUserId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean allCompletedByProject(UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from work_order
        where project_id = ?
          and deleted_at is null
        """,
        Integer.class,
        projectId
    );
    Integer incompleteCount = jdbcTemplate.queryForObject(
        """
        select count(*)
        from work_order
        where project_id = ?
          and status <> 'COMPLETED'
          and deleted_at is null
        """,
        Integer.class,
        projectId
    );
    return count != null && count > 0 && incompleteCount != null && incompleteCount == 0;
  }

  private WorkOrderRecord record(ResultSet rs) throws SQLException {
    return new WorkOrderRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("study_plan_id", UUID.class),
        rs.getObject("project_id", UUID.class),
        rs.getObject("sample_id", UUID.class),
        rs.getObject("owner_user_id", UUID.class),
        rs.getString("status"),
        rs.getInt("progress_percent"),
        instant(rs.getTimestamp("planned_finish_at"))
    );
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
