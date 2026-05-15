package com.njxs.lims.workcenter.persistence;

import com.njxs.lims.workcenter.domain.WorkCenterProjectRecord;
import com.njxs.lims.workcenter.domain.WorkCenterResourceLoadRecord;
import com.njxs.lims.workcenter.domain.WorkCenterScope;
import com.njxs.lims.workcenter.domain.WorkCenterSummaryRecord;
import com.njxs.lims.workcenter.domain.WorkCenterTaskRecord;
import com.njxs.lims.workcenter.domain.WorkCenterTrendRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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

  @Override
  public List<WorkCenterTaskRecord> tasks(WorkCenterScope scope, int limit) {
    List<WorkCenterTaskRecord> tasks = new ArrayList<>();

    if (scope.allAccess() || scope.hasRole("SALES_REP")) {
      tasks.addAll(queryTasks(
          """
          select ('SUBMISSION_ACCEPT:' || sub.id::text) task_key,
                 'SALES' role_group,
                 'SUBMISSION_ACCEPT' task_type,
                 '受理客户送检单' title,
                 sub.biz_no biz_no,
                 co.name_zh customer_org_name,
                 sub.status status,
                 95 priority,
                 sub.created_at due_at,
                 '/internal/submissions' action_path
          from submission sub
          left join customer_org co on co.id = sub.customer_org_id and co.deleted_at is null
          where sub.deleted_at is null
            and sub.status = 'SUBMITTED'
            and (? = true or sub.assigned_sales_user_id = ?)
          order by sub.created_at asc
          limit 12
          """,
          scope.allAccess(),
          scope.userId()
      ));
      tasks.addAll(queryTasks(
          """
          select ('PROJECT_QUOTATION:' || p.id::text) task_key,
                 'SALES' role_group,
                 'PROJECT_QUOTATION' task_type,
                 '推进报价或合同' title,
                 p.biz_no biz_no,
                 co.name_zh customer_org_name,
                 p.status status,
                 85 priority,
                 p.updated_at due_at,
                 '/internal/projects' action_path
          from project p
          join submission sub on sub.id = p.submission_id and sub.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where p.deleted_at is null
            and p.status in ('ACCEPTED', 'QUOTABLE', 'QUOTED')
            and (? = true or sub.assigned_sales_user_id = ?)
          order by p.updated_at asc
          limit 12
          """,
          scope.allAccess(),
          scope.userId()
      ));
    }

    if (scope.allAccess() || scope.hasRole("TECHNICAL_LEADER")) {
      tasks.addAll(queryTasks(
          """
          select ('NONSTANDARD_TECH:' || p.id::text) task_key,
                 'TECHNICAL' role_group,
                 'NONSTANDARD_TECH_REVIEW' task_type,
                 '非标技术评审' title,
                 p.biz_no biz_no,
                 co.name_zh customer_org_name,
                 p.status status,
                 92 priority,
                 p.updated_at due_at,
                 '/internal/projects' action_path
          from project p
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where p.deleted_at is null
            and p.status = 'NONSTANDARD_REVIEW'
            and p.requires_nonstandard_review = true
            and (? = true or p.technical_owner_id = ?)
            and not exists (
              select 1
              from approval_record ar
              where ar.entity_type = 'PROJECT'
                and ar.entity_id = p.id
                and ar.stage = 'TECH'
                and ar.outcome = 'APPROVED'
            )
          order by p.updated_at asc
          limit 12
          """,
          scope.allAccess(),
          scope.userId()
      ));
    }

    if (scope.allAccess() || scope.hasRole("BUSINESS_OWNER")) {
      tasks.addAll(queryTasks(
          """
          select ('NONSTANDARD_BUSINESS:' || p.id::text) task_key,
                 'BUSINESS' role_group,
                 'NONSTANDARD_BUSINESS_REVIEW' task_type,
                 '非标业务评审' title,
                 p.biz_no biz_no,
                 co.name_zh customer_org_name,
                 p.status status,
                 91 priority,
                 p.updated_at due_at,
                 '/internal/projects' action_path
          from project p
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where p.deleted_at is null
            and p.status = 'NONSTANDARD_REVIEW'
            and p.requires_nonstandard_review = true
            and (? = true or p.business_owner_id = ?)
            and not exists (
              select 1
              from approval_record ar
              where ar.entity_type = 'PROJECT'
                and ar.entity_id = p.id
                and ar.stage = 'BUSINESS'
                and ar.outcome = 'APPROVED'
            )
          order by p.updated_at asc
          limit 12
          """,
          scope.allAccess(),
          scope.userId()
      ));
    }

    if (scope.allAccess() || scope.hasRole("SAMPLE_MANAGER")) {
      tasks.addAll(queryTasks(
          """
          select ('SAMPLE_FLOW:' || s.id::text) task_key,
                 'SAMPLE' role_group,
                 'SAMPLE_FLOW' task_type,
                 case s.status
                   when 'ARRIVED_PENDING_CHECK' then '核验到达样品'
                   when 'PRE_IN_STOCK' then '样品入库'
                   else '样品流转处理'
                 end title,
                 s.biz_no biz_no,
                 co.name_zh customer_org_name,
                 s.status status,
                 82 priority,
                 s.updated_at due_at,
                 '/internal/samples/receiving' action_path
          from sample s
          join project p on p.id = s.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where s.deleted_at is null
            and s.status in ('ARRIVED_PENDING_CHECK', 'PRE_IN_STOCK', 'PENDING_RETURN', 'PENDING_DISPOSAL')
          order by s.updated_at asc
          limit 12
          """
      ));
      tasks.addAll(queryTasks(
          """
          select ('SAMPLE_RETENTION:' || sr.id::text) task_key,
                 'SAMPLE' role_group,
                 'SAMPLE_RETENTION' task_type,
                 '留样登记或处置' title,
                 sr.biz_no biz_no,
                 co.name_zh customer_org_name,
                 sr.status status,
                 78 priority,
                 sr.retain_until::timestamptz due_at,
                 '/internal/sample-retentions' action_path
          from sample_retention sr
          join project p on p.id = sr.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where sr.deleted_at is null
            and (
              sr.status in ('PENDING_REGISTER', 'PENDING_DISPOSAL', 'EXPIRING')
              or sr.retain_until <= current_date + interval '30 days'
            )
          order by sr.retain_until asc
          limit 12
          """
      ));
    }

    if (scope.allAccess() || scope.hasRole("INSPECTOR")) {
      tasks.addAll(queryTasks(
          """
          select ('WORK_ORDER:' || wo.id::text) task_key,
                 'INSPECTOR' role_group,
                 'WORK_ORDER_EXECUTION' task_type,
                 '执行本人工作单' title,
                 wo.biz_no biz_no,
                 co.name_zh customer_org_name,
                 wo.status status,
                 88 priority,
                 coalesce(wo.planned_finish_at, wo.updated_at) due_at,
                 '/internal/work-orders' action_path
          from work_order wo
          join project p on p.id = wo.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where wo.deleted_at is null
            and wo.status <> 'COMPLETED'
            and (? = true or wo.owner_user_id = ?)
          order by coalesce(wo.planned_finish_at, wo.updated_at) asc
          limit 12
          """,
          scope.allAccess(),
          scope.userId()
      ));
    }

    if (scope.allAccess() || scope.hasRole("QC")) {
      tasks.addAll(queryTasks(
          """
          select ('RAW_RECORD_QC:' || rr.id::text) task_key,
                 'QC' role_group,
                 'RAW_RECORD_REVIEW' task_type,
                 '复核原始记录' title,
                 rr.biz_no biz_no,
                 co.name_zh customer_org_name,
                 rr.status status,
                 90 priority,
                 rr.submitted_at due_at,
                 '/internal/work-orders' action_path
          from raw_record rr
          join project p on p.id = rr.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where rr.deleted_at is null
            and rr.status = 'REVIEWING'
          order by rr.submitted_at asc
          limit 12
          """
      ));
      tasks.addAll(reportTasks("QC", "REPORT_QC_REVIEW", "报告 QC 审核", "QC_REVIEWING", 89));
    }

    if (scope.allAccess() || scope.hasRole("QA")) {
      tasks.addAll(reportTasks("QA", "REPORT_QA_REVIEW", "报告 QA 审核", "QA_REVIEWING", 87));
    }

    if (scope.allAccess() || scope.hasRole("SD")) {
      tasks.addAll(queryTasks(
          """
          select ('STUDY_PLAN_SD:' || sp.id::text) task_key,
                 'SD' role_group,
                 'STUDY_PLAN_REVIEW' task_type,
                 '方案审核或下发' title,
                 sp.biz_no biz_no,
                 co.name_zh customer_org_name,
                 sp.status status,
                 86 priority,
                 coalesce(sp.planned_start_at, sp.updated_at) due_at,
                 '/internal/study-plans' action_path
          from study_plan sp
          join project p on p.id = sp.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where sp.deleted_at is null
            and sp.status in ('REVIEWING', 'PENDING_DISPATCH', 'READY_TO_DISPATCH')
            and (? = true or p.sd_user_id = ?)
          order by coalesce(sp.planned_start_at, sp.updated_at) asc
          limit 12
          """,
          scope.allAccess(),
          scope.userId()
      ));
      tasks.addAll(reportTasks("SD", "REPORT_SD_REVIEW", "报告 SD 审核", "SD_REVIEWING", 86));
    }

    if (scope.allAccess() || scope.hasRole("REPORT_PREPARER")) {
      tasks.addAll(queryTasks(
          """
          select ('REPORT_DRAFT:' || p.id::text) task_key,
                 'REPORT' role_group,
                 'REPORT_DRAFT' task_type,
                 '生成或提交报告草稿' title,
                 p.biz_no biz_no,
                 co.name_zh customer_org_name,
                 p.status status,
                 84 priority,
                 p.updated_at due_at,
                 '/internal/reports' action_path
          from project p
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where p.deleted_at is null
            and p.status = 'REPORTING'
          order by p.updated_at asc
          limit 12
          """
      ));
    }

    if (scope.allAccess() || scope.hasRole("AUTHORIZED_SIGNATORY")) {
      tasks.addAll(reportTasks(
          "SIGNATORY",
          "REPORT_ISSUE",
          "授权签发正式报告",
          "PENDING_ISSUE",
          93
      ));
    }

    if (scope.allAccess() || scope.hasAnyRole("FINANCE_CASHIER", "FINANCE_MANAGER")) {
      tasks.addAll(queryTasks(
          """
          select ('PAYMENT_GATE:' || r.id::text) task_key,
                 'FINANCE' role_group,
                 'PAYMENT_UNLOCK' task_type,
                 '登记收款并解锁报告' title,
                 r.biz_no biz_no,
                 co.name_zh customer_org_name,
                 r.payment_gate_status status,
                 94 priority,
                 r.issued_at due_at,
                 '/internal/reports' action_path
          from report r
          join project p on p.id = r.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where r.deleted_at is null
            and r.status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED')
            and r.payment_gate_status <> 'UNLOCKED'
          order by r.issued_at asc nulls last, r.updated_at asc
          limit 12
          """
      ));
    }

    if (scope.allAccess() || scope.hasAnyRole("DEPT_MANAGER", "GENERAL_MANAGER")) {
      String status = scope.hasRole("GENERAL_MANAGER") ? "MANAGEMENT_REVIEWING" : "DEPT_REVIEWING";
      tasks.addAll(queryTasks(
          """
          select ('GREEN_CHANNEL:' || gc.id::text) task_key,
                 'GREEN_CHANNEL' role_group,
                 'GREEN_CHANNEL_APPROVAL' task_type,
                 '绿色通道审批' title,
                 gc.biz_no biz_no,
                 co.name_zh customer_org_name,
                 gc.status status,
                 90 priority,
                 gc.requested_at due_at,
                 '/internal/green-channel-requests' action_path
          from green_channel_request gc
          join project p on p.id = gc.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where gc.deleted_at is null
            and (
              (? = true and gc.status in ('DEPT_REVIEWING', 'MANAGEMENT_REVIEWING'))
              or (? = false and gc.status = ?)
            )
          order by gc.requested_at asc
          limit 12
          """,
          scope.allAccess(),
          scope.allAccess(),
          status
      ));
    }

    if (scope.allAccess() || scope.hasRole("EQUIPMENT_MANAGER")) {
      tasks.addAll(queryTasks(
          """
          select ('DEVICE_BOOKING:' || db.id::text) task_key,
                 'EQUIPMENT' role_group,
                 'DEVICE_BOOKING' task_type,
                 '处理设备预约' title,
                 db.biz_no biz_no,
                 d.name customer_org_name,
                 db.status status,
                 76 priority,
                 db.start_at due_at,
                 '/internal/devices/calendar' action_path
          from device_booking db
          join device d on d.id = db.device_id and d.deleted_at is null
          where db.deleted_at is null
            and db.status in ('BOOKED', 'NEEDS_RESCHEDULE')
          order by db.start_at asc
          limit 12
          """
      ));
      tasks.addAll(queryTasks(
          """
          select ('DEVICE_FILE:' || df.id::text) task_key,
                 'EQUIPMENT' role_group,
                 'DEVICE_FILE_EXPIRING' task_type,
                 '设备文件临期复核' title,
                 d.device_no biz_no,
                 d.name customer_org_name,
                 df.status status,
                 74 priority,
                 df.valid_until::timestamptz due_at,
                 '/internal/devices/files' action_path
          from device_file df
          join device d on d.id = df.device_id and d.deleted_at is null
          where df.deleted_at is null
            and df.status = 'ACTIVE'
            and df.valid_until <= current_date + interval '30 days'
          order by df.valid_until asc
          limit 12
          """
      ));
    }

    if (scope.allAccess() || scope.hasRole("INVENTORY_MANAGER")) {
      tasks.addAll(queryTasks(
          """
          select ('INVENTORY:' || ib.id::text) task_key,
                 'INVENTORY' role_group,
                 'INVENTORY_ATTENTION' task_type,
                 '库存预警处理' title,
                 ib.batch_no biz_no,
                 ib.name customer_org_name,
                 ib.status status,
                 72 priority,
                 ib.expiry_date::timestamptz due_at,
                 '/internal/inventory' action_path
          from inventory_batch ib
          where ib.deleted_at is null
            and (
              ib.status <> 'IN_STOCK'
              or ib.quantity <= 2
              or ib.expiry_date <= current_date + interval '30 days'
            )
          order by ib.expiry_date asc nulls last, ib.quantity asc
          limit 12
          """
      ));
    }

    if (scope.allAccess() || scope.hasRole("ARCHIVE_MANAGER")) {
      tasks.addAll(queryTasks(
          """
          select ('ARCHIVE_REPORT:' || r.id::text) task_key,
                 'ARCHIVE' role_group,
                 'ARCHIVE_REPORT' task_type,
                 '核对正式报告归档' title,
                 r.biz_no biz_no,
                 co.name_zh customer_org_name,
                 r.status status,
                 70 priority,
                 r.released_at due_at,
                 '/internal/archive' action_path
          from report r
          join project p on p.id = r.project_id and p.deleted_at is null
          left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
          where r.deleted_at is null
            and r.is_official = true
            and r.status in ('GREEN_RELEASED', 'RELEASED')
            and not exists (
              select 1
              from archive_item ai
              where ai.entity_type = 'REPORT'
                and ai.entity_id = r.id
                and ai.deleted_at is null
            )
          order by r.released_at asc nulls last, r.updated_at asc
          limit 12
          """
      ));
    }

    return tasks.stream()
        .sorted(Comparator
            .comparingInt(WorkCenterTaskRecord::priority).reversed()
            .thenComparing(task -> task.dueAt() == null ? Instant.MAX : task.dueAt()))
        .limit(limit)
        .toList();
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

  private List<WorkCenterTaskRecord> reportTasks(
      String roleGroup,
      String taskType,
      String title,
      String status,
      int priority
  ) {
    return queryTasks(
        """
        select (? || ':' || r.id::text) task_key,
               ? role_group,
               ? task_type,
               ? title,
               r.biz_no biz_no,
               co.name_zh customer_org_name,
               r.status status,
               ? priority,
               coalesce(r.issued_at, r.updated_at) due_at,
               '/internal/reports' action_path
        from report r
        join project p on p.id = r.project_id and p.deleted_at is null
        left join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
        where r.deleted_at is null
          and r.status = ?
        order by coalesce(r.issued_at, r.updated_at) asc
        limit 12
        """,
        taskType,
        roleGroup,
        taskType,
        title,
        priority,
        status
    );
  }

  private List<WorkCenterTaskRecord> queryTasks(String sql, Object... args) {
    return jdbcTemplate.query(sql, (rs, rowNum) -> task(rs), args);
  }

  private WorkCenterTaskRecord task(ResultSet rs) throws SQLException {
    return new WorkCenterTaskRecord(
        rs.getString("task_key"),
        rs.getString("role_group"),
        rs.getString("task_type"),
        rs.getString("title"),
        rs.getString("biz_no"),
        rs.getString("customer_org_name"),
        rs.getString("status"),
        rs.getInt("priority"),
        instant(rs.getTimestamp("due_at")),
        rs.getString("action_path")
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
