package com.njxs.lims.workcenter.persistence;

import com.njxs.lims.workcenter.domain.WorkCenterProjectRecord;
import com.njxs.lims.workcenter.domain.WorkCenterResourceLoadRecord;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import com.njxs.lims.workcenter.domain.WorkCenterSummaryRecord;
import com.njxs.lims.workcenter.domain.WorkCenterTrendRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcWorkCenterRepository implements WorkCenterRepository {

  private static final String VISIBLE_PROJECTS = """
      select p.*
      from project p
      join submission sub on sub.id = p.submission_id and sub.deleted_at is null
      where p.deleted_at is null
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
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcWorkCenterRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public WorkCenterSummaryRecord summary(WorkCenterScope scope) {
    return jdbcTemplate.queryForObject(
        "with visible_projects as (" + VISIBLE_PROJECTS + ") " + """
        select
          (select count(*) from visible_projects where health_status = 'NORMAL') normal_projects,
          (select count(*) from visible_projects where health_status = 'WARNING') warning_projects,
          (select count(*) from visible_projects where health_status = 'RISK') risk_projects,
          (
            select count(*)
            from work_order wo
            where wo.owner_user_id = ?
              and wo.deleted_at is null
              and wo.status <> 'COMPLETED'
          ) my_todos,
          (
            select count(*)
            from work_order wo
            join visible_projects p on p.id = wo.project_id
            where wo.deleted_at is null
              and wo.status <> 'COMPLETED'
              and wo.planned_finish_at is not null
              and wo.planned_finish_at < now()
          ) overdue_tasks,
          now() generated_at
        """,
        (rs, rowNum) -> new WorkCenterSummaryRecord(
            rs.getInt("normal_projects"),
            rs.getInt("warning_projects"),
            rs.getInt("risk_projects"),
            rs.getInt("my_todos"),
            rs.getInt("overdue_tasks"),
            instant(rs.getTimestamp("generated_at"))
        ),
        scopedArgs(scope, scope.userId()).toArray()
    );
  }

  @Override
  public List<WorkCenterProjectRecord> projects(WorkCenterScope scope, String filter, int limit, int offset) {
    List<Object> args = scopedArgs(scope);
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(
        "with visible_projects as (" + VISIBLE_PROJECTS + ") " + """
        select p.id project_id,
               p.biz_no project_biz_no,
               co.name_zh customer_org_name,
               p.status,
               p.health_status,
               p.health_score,
               (
                 select wb.description
                 from work_center_blocker wb
                 where wb.project_id = p.id
                   and wb.deleted_at is null
                   and wb.status <> 'CLOSED'
                 order by
                   case wb.severity when 'HIGH' then 1 when 'MEDIUM' then 2 else 3 end,
                   wb.expected_recovery_at asc
                 limit 1
               ) main_blocker
        from visible_projects p
        left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
        """ + projectFilter(filter) + """
        order by
          case p.health_status when 'RISK' then 1 when 'WARNING' then 2 else 3 end,
          p.health_score asc,
          p.updated_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> project(rs),
        args.toArray()
    );
  }

  @Override
  public int countProjects(WorkCenterScope scope, String filter) {
    Integer count = jdbcTemplate.queryForObject(
        "with visible_projects as (" + VISIBLE_PROJECTS + ") " + """
        select count(*)
        from visible_projects p
        """ + projectFilter(filter),
        Integer.class,
        scopedArgs(scope).toArray()
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<WorkCenterResourceLoadRecord> resourceLoad(WorkCenterScope scope) {
    return jdbcTemplate.query(
        "with visible_projects as (" + VISIBLE_PROJECTS + ") " + """
        select u.id user_id,
               u.display_name,
               count(wo.id) active_work_orders,
               least(100, count(wo.id) * 20)::int load_percent
        from work_order wo
        join visible_projects p on p.id = wo.project_id
        join app_user u on u.id = wo.owner_user_id
        where wo.deleted_at is null
          and wo.status <> 'COMPLETED'
        group by u.id, u.display_name
        order by active_work_orders desc, u.display_name asc
        limit 20
        """,
        (rs, rowNum) -> new WorkCenterResourceLoadRecord(
            rs.getObject("user_id", UUID.class),
            rs.getString("display_name"),
            rs.getInt("active_work_orders"),
            rs.getInt("load_percent")
        ),
        scopedArgs(scope).toArray()
    );
  }

  @Override
  public List<WorkCenterTrendRecord> trends(WorkCenterScope scope) {
    return jdbcTemplate.query(
        "with visible_projects as (" + VISIBLE_PROJECTS + "), metrics as (" + """
        select 'plannedTasks' metric_code,
               current_date metric_date,
               count(*)::numeric value
        from work_order wo
        join visible_projects p on p.id = wo.project_id
        where wo.deleted_at is null
          and wo.planned_finish_at is not null
        union all
        select 'completedTasks', current_date, count(*)::numeric
        from work_order wo
        join visible_projects p on p.id = wo.project_id
        where wo.deleted_at is null
          and wo.status = 'COMPLETED'
        union all
        select 'overdueTasks', current_date, count(*)::numeric
        from work_order wo
        join visible_projects p on p.id = wo.project_id
        where wo.deleted_at is null
          and wo.status <> 'COMPLETED'
          and wo.planned_finish_at is not null
          and wo.planned_finish_at < now()
        union all
        select 'openBlockers', current_date, count(*)::numeric
        from work_center_blocker wb
        join visible_projects p on p.id = wb.project_id
        where wb.deleted_at is null
          and wb.status <> 'CLOSED'
        union all
        select 'riskProjects', current_date, count(*)::numeric
        from visible_projects
        where health_status = 'RISK'
        union all
        select 'resourceLoadRate', current_date, coalesce(avg(load_percent), 0)::numeric
        from (
          select least(100, count(wo.id) * 20)::int load_percent
          from work_order wo
          join visible_projects p on p.id = wo.project_id
          where wo.deleted_at is null
            and wo.status <> 'COMPLETED'
          group by wo.owner_user_id
        ) load
        )
        select metric_code, metric_date, value
        from metrics
        order by metric_code
        """,
        (rs, rowNum) -> new WorkCenterTrendRecord(
            rs.getString("metric_code"),
            localDate(rs.getDate("metric_date")),
            rs.getBigDecimal("value")
        ),
        scopedArgs(scope).toArray()
    );
  }

  private WorkCenterProjectRecord project(ResultSet rs) throws SQLException {
    return new WorkCenterProjectRecord(
        rs.getObject("project_id", UUID.class),
        rs.getString("project_biz_no"),
        rs.getString("customer_org_name"),
        rs.getString("status"),
        rs.getString("health_status"),
        rs.getInt("health_score"),
        rs.getString("main_blocker")
    );
  }

  private String projectFilter(String filter) {
    return switch (filter == null ? "" : filter.toLowerCase()) {
      case "risk" -> " where p.health_status = 'RISK'\n";
      case "warning" -> " where p.health_status = 'WARNING'\n";
      case "normal" -> " where p.health_status = 'NORMAL'\n";
      case "overdue" -> """
          where exists (
            select 1
            from work_order wo
            where wo.project_id = p.id
              and wo.deleted_at is null
              and wo.status <> 'COMPLETED'
              and wo.planned_finish_at is not null
              and wo.planned_finish_at < now()
          )
          """;
      default -> "";
    };
  }

  private List<Object> scopedArgs(WorkCenterScope scope, Object... extraArgs) {
    List<Object> args = new ArrayList<>(List.of(
        scope.allAccess(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId(),
        scope.userId()
    ));
    args.addAll(List.of(extraArgs));
    return args;
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private LocalDate localDate(Date date) {
    return date == null ? null : date.toLocalDate();
  }
}
