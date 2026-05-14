package com.njxs.lims.report.snapshot.persistence;

import com.njxs.lims.report.snapshot.domain.ReportSnapshotSource;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcReportSnapshotSourceRepository implements ReportSnapshotSourceRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcReportSnapshotSourceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<ReportSnapshotSource> findByReportId(UUID reportId) {
    return jdbcTemplate.query(
        """
        select r.id report_id,
               r.biz_no report_biz_no,
               r.status report_status,
               r.watermark_type,
               r.payment_gate_status,
               co.name_zh customer_org_name_zh,
               co.address_zh customer_org_address_zh,
               m.name_zh manufacturer_name_zh,
               m.address_zh manufacturer_address_zh,
               wo.id work_order_id,
               s.id sample_id,
               s.biz_no sample_biz_no,
               s.sample_name_zh,
               s.lot_batch,
               s.model_ref,
               s.size_spec,
               s.quantity,
               s.quantity_unit,
               s.physical_state,
               s.received_at,
               s.received_date,
               rr.id raw_record_id,
               rr.test_item raw_record_test_item,
               rr.test_basis raw_record_test_basis,
               rr.test_time raw_record_test_time,
               rr.record_data raw_record_data
        from report r
        join project p on p.id = r.project_id and p.deleted_at is null
        join customer_org co on co.id = p.customer_org_id and co.deleted_at is null
        join submission sub on sub.id = p.submission_id and sub.deleted_at is null
        join manufacturer m on m.id = sub.manufacturer_id and m.deleted_at is null
        left join report_work_order rwo on rwo.report_id = r.id
        left join work_order wo on wo.id = rwo.work_order_id and wo.deleted_at is null
        left join sample s on s.id = wo.sample_id and s.deleted_at is null
        left join raw_record rr on rr.id = coalesce(
          rwo.raw_record_id,
          (
            select id
            from raw_record
            where work_order_id = wo.id
              and status = 'APPROVED'
              and deleted_at is null
            order by submitted_at desc
            limit 1
          )
        ) and rr.deleted_at is null
        where r.id = ? and r.deleted_at is null
        order by rr.submitted_at desc nulls last
        limit 1
        """,
        (rs, rowNum) -> source(rs),
        reportId
    ).stream().findFirst();
  }

  private ReportSnapshotSource source(ResultSet rs) throws SQLException {
    return new ReportSnapshotSource(
        rs.getObject("report_id", UUID.class),
        rs.getString("report_biz_no"),
        rs.getString("report_status"),
        rs.getString("watermark_type"),
        rs.getString("payment_gate_status"),
        rs.getString("customer_org_name_zh"),
        rs.getString("customer_org_address_zh"),
        rs.getString("manufacturer_name_zh"),
        rs.getString("manufacturer_address_zh"),
        rs.getObject("work_order_id", UUID.class),
        rs.getObject("sample_id", UUID.class),
        rs.getString("sample_biz_no"),
        rs.getString("sample_name_zh"),
        rs.getString("lot_batch"),
        rs.getString("model_ref"),
        rs.getString("size_spec"),
        rs.getBigDecimal("quantity"),
        rs.getString("quantity_unit"),
        rs.getString("physical_state"),
        instant(rs.getTimestamp("received_at")),
        localDate(rs.getDate("received_date")),
        rs.getObject("raw_record_id", UUID.class),
        rs.getString("raw_record_test_item"),
        rs.getString("raw_record_test_basis"),
        rs.getString("raw_record_test_time"),
        rs.getString("raw_record_data")
    );
  }

  private Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private LocalDate localDate(Date date) {
    return date == null ? null : date.toLocalDate();
  }
}
