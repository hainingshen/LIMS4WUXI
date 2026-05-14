package com.njxs.lims.workcenter.persistence;

import com.njxs.lims.workcenter.domain.ProjectHealth;
import com.njxs.lims.workcenter.domain.ProjectHealthContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcProjectHealthRepository implements ProjectHealthRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcProjectHealthRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<ProjectHealthContext> context(UUID projectId) {
    return jdbcTemplate.query(
        """
        select p.id project_id,
               coalesce(max(wo.last_progress_at), p.updated_at) last_progress_at,
               count(*) filter (
                 where wo.planned_finish_at is not null
                   and wo.planned_finish_at < now()
                   and wo.status <> 'COMPLETED'
               ) overdue_task_count,
               false forced_risk
        from project p
        left join work_order wo on wo.project_id = p.id
          and wo.deleted_at is null
        where p.id = ?
          and p.deleted_at is null
        group by p.id
        """,
        (rs, rowNum) -> context(rs),
        projectId
    ).stream().findFirst();
  }

  @Override
  public int configInt(String key, int defaultValue) {
    return jdbcTemplate.query(
        """
        select config_value
        from system_config
        where config_key = ?
        """,
        (rs, rowNum) -> Integer.parseInt(rs.getString("config_value")),
        key
    ).stream().findFirst().orElse(defaultValue);
  }

  @Override
  public void updateHealth(UUID projectId, ProjectHealth health) {
    jdbcTemplate.update(
        """
        update project
        set health_score = ?,
            health_status = ?,
            updated_at = now(),
            version = version + 1
        where id = ?
          and deleted_at is null
        """,
        health.score(),
        health.status(),
        projectId
    );
  }

  private ProjectHealthContext context(ResultSet rs) throws SQLException {
    return new ProjectHealthContext(
        rs.getObject("project_id", UUID.class),
        instant(rs.getTimestamp("last_progress_at")),
        rs.getInt("overdue_task_count"),
        rs.getBoolean("forced_risk")
    );
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
