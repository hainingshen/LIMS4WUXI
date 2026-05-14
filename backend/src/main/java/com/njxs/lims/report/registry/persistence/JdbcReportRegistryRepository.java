package com.njxs.lims.report.registry.persistence;

import com.njxs.lims.report.registry.domain.ReportRegistryItemRecord;
import com.njxs.lims.report.registry.domain.ReportRegistryVersionRecord;
import com.njxs.lims.report.registry.service.ReportRegistryQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcReportRegistryRepository implements ReportRegistryRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcReportRegistryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<ReportRegistryItemRecord> search(ReportRegistryQuery query, int limit, int offset) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder("""
        select r.id report_id,
               r.biz_no report_biz_no,
               p.id project_id,
               p.biz_no project_biz_no,
               p.customer_org_id,
               r.template_code,
               r.template_version,
               r.current_version_no,
               r.status,
               r.issued_at,
               r.released_at,
               r.payment_gate_status,
               (
                 select count(*)
                 from report_delivery_log dl
                 where dl.report_id = r.id
                   and dl.deleted_at is null
               ) delivery_count
        from report r
        join project p on p.id = r.project_id
        left join report_template rt on rt.id = r.report_template_id
        where r.deleted_at is null
          and p.deleted_at is null
        """);
    appendFilters(sql, args, query);
    sql.append(" order by r.issued_at desc nulls last, r.biz_no desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> item(rs), args.toArray());
  }

  @Override
  public int count(ReportRegistryQuery query) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder("""
        select count(*)
        from report r
        join project p on p.id = r.project_id
        left join report_template rt on rt.id = r.report_template_id
        where r.deleted_at is null
          and p.deleted_at is null
        """);
    appendFilters(sql, args, query);
    Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
    return count == null ? 0 : count;
  }

  @Override
  public List<ReportRegistryVersionRecord> versions(List<UUID> reportIds) {
    if (reportIds.isEmpty()) {
      return List.of();
    }
    return jdbcTemplate.query(
        """
        select id, report_id, version_no, status, docx_file_id, pdf_file_id, issued_at
        from report_version
        where report_id in (%s)
          and deleted_at is null
        order by report_id, version_no desc
        """.formatted(placeholders(reportIds.size())),
        (rs, rowNum) -> version(rs),
        reportIds.toArray()
    );
  }

  private void appendFilters(StringBuilder sql, List<Object> args, ReportRegistryQuery query) {
    if (query.keyword() != null && !query.keyword().isBlank()) {
      sql.append("""
          and (
            lower(r.biz_no) like ?
            or lower(p.biz_no) like ?
            or lower(r.template_code) like ?
            or exists (
              select 1
              from customer_org co
              where co.id = p.customer_org_id
                and co.deleted_at is null
                and lower(co.name_zh) like ?
            )
          )
          """);
      String keyword = "%" + query.keyword().trim().toLowerCase() + "%";
      args.add(keyword);
      args.add(keyword);
      args.add(keyword);
      args.add(keyword);
    }
    if (query.customerOrgId() != null) {
      sql.append(" and p.customer_org_id = ?");
      args.add(query.customerOrgId());
    }
    if (query.projectId() != null) {
      sql.append(" and p.id = ?");
      args.add(query.projectId());
    }
    if (query.reportBizNo() != null && !query.reportBizNo().isBlank()) {
      sql.append(" and lower(r.biz_no) = ?");
      args.add(query.reportBizNo().trim().toLowerCase());
    }
    if (query.templateType() != null && !query.templateType().isBlank()) {
      sql.append(" and rt.template_type = ?");
      args.add(query.templateType().trim().toUpperCase());
    }
    if (query.status() != null && !query.status().isBlank()) {
      sql.append(" and r.status = ?");
      args.add(query.status().trim().toUpperCase());
    }
    if (query.issuedFrom() != null) {
      sql.append(" and r.issued_at >= ?");
      args.add(Timestamp.from(query.issuedFrom()));
    }
    if (query.issuedTo() != null) {
      sql.append(" and r.issued_at <= ?");
      args.add(Timestamp.from(query.issuedTo()));
    }
  }

  private ReportRegistryItemRecord item(ResultSet rs) throws SQLException {
    return new ReportRegistryItemRecord(
        rs.getObject("report_id", UUID.class),
        rs.getString("report_biz_no"),
        rs.getObject("project_id", UUID.class),
        rs.getString("project_biz_no"),
        rs.getObject("customer_org_id", UUID.class),
        rs.getString("template_code"),
        rs.getString("template_version"),
        rs.getInt("current_version_no"),
        rs.getString("status"),
        instant(rs.getTimestamp("issued_at")),
        instant(rs.getTimestamp("released_at")),
        rs.getString("payment_gate_status"),
        rs.getInt("delivery_count"),
        List.of()
    );
  }

  private ReportRegistryVersionRecord version(ResultSet rs) throws SQLException {
    return new ReportRegistryVersionRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("report_id", UUID.class),
        rs.getInt("version_no"),
        rs.getString("status"),
        rs.getObject("docx_file_id", UUID.class),
        rs.getObject("pdf_file_id", UUID.class),
        instant(rs.getTimestamp("issued_at"))
    );
  }

  private String placeholders(int size) {
    return String.join(", ", java.util.Collections.nCopies(size, "?"));
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
