package com.njxs.lims.report.snapshot.persistence;

import com.njxs.lims.report.snapshot.domain.ReportFieldMapping;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcReportFieldMappingRepository implements ReportFieldMappingRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcReportFieldMappingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<ReportFieldMapping> findByTemplateCode(String templateCode) {
    return jdbcTemplate.query(
        """
        select target_field, report_label, source_entity, source_field_or_rule,
               required, auto_fill, notes, sort_order
        from report_field_mapping
        where template_code = ?
        order by sort_order
        """,
        (rs, rowNum) -> new ReportFieldMapping(
            rs.getString("target_field"),
            rs.getString("report_label"),
            rs.getString("source_entity"),
            rs.getString("source_field_or_rule"),
            rs.getBoolean("required"),
            rs.getBoolean("auto_fill"),
            rs.getString("notes"),
            rs.getInt("sort_order")
        ),
        templateCode
    );
  }
}
