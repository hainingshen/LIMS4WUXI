package com.njxs.lims.payment.persistence;

import com.njxs.lims.payment.domain.PaymentReceiptRecord;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcPaymentReceiptRepository implements PaymentReceiptRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, report_id, amount, received_at, received_by,
      receipt_method, voucher_file_id, note, status
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcPaymentReceiptRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextBizNo(int year) {
    String prefix = "PAY-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from payment_receipt where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public PaymentReceiptRecord save(PaymentReceiptRecord record, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into payment_receipt (
          id, biz_no, project_id, report_id, amount, received_at, received_by,
          receipt_method, voucher_file_id, note, status,
          created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, 'NEW', 100)
        """,
        record.id(),
        record.bizNo(),
        record.projectId(),
        record.reportId(),
        record.amount(),
        Timestamp.from(record.receivedAt()),
        record.receivedBy(),
        record.method(),
        record.voucherFileId(),
        record.note(),
        record.status(),
        actorUserId,
        actorUserId
    );
    return record;
  }

  @Override
  public List<PaymentReceiptRecord> list(UUID projectId, int limit, int offset) {
    if (projectId == null) {
      return jdbcTemplate.query(
          "select " + SELECT_COLUMNS + """
          from payment_receipt
          where deleted_at is null
          order by received_at desc, biz_no desc
          limit ? offset ?
          """,
          (rs, rowNum) -> record(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from payment_receipt
        where project_id = ?
          and deleted_at is null
        order by received_at desc, biz_no desc
        limit ? offset ?
        """,
        (rs, rowNum) -> record(rs),
        projectId,
        limit,
        offset
    );
  }

  @Override
  public int count(UUID projectId) {
    Integer count;
    if (projectId == null) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from payment_receipt where deleted_at is null",
          Integer.class
      );
    } else {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from payment_receipt
          where project_id = ?
            and deleted_at is null
          """,
          Integer.class,
          projectId
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public BigDecimal recordedAmountForReport(UUID reportId) {
    BigDecimal amount = jdbcTemplate.queryForObject(
        """
        select coalesce(sum(pr.amount), 0)
        from report r
        join payment_receipt pr
          on pr.project_id = r.project_id
         and (pr.report_id is null or pr.report_id = r.id)
         and pr.status = 'RECORDED'
         and pr.deleted_at is null
        where r.id = ?
          and r.deleted_at is null
        """,
        BigDecimal.class,
        reportId
    );
    return amount == null ? BigDecimal.ZERO : amount;
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

  @Override
  public boolean reportBelongsToProject(UUID reportId, UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from report
        where id = ?
          and project_id = ?
          and deleted_at is null
        """,
        Integer.class,
        reportId,
        projectId
    );
    return count != null && count > 0;
  }

  @Override
  public void markProjectPaid(UUID projectId, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update project
        set payment_status = 'PAID',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        """,
        actorUserId,
        projectId
    );
  }

  @Override
  public void unlockReportsByProject(UUID projectId, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update report
        set payment_gate_status = 'UNLOCKED',
            status = case
              when status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED') then 'RELEASED'
              else status
            end,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where project_id = ?
          and status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED', 'RELEASED')
          and deleted_at is null
        """,
        actorUserId,
        projectId
    );
  }

  @Override
  public void unlockReport(UUID reportId, UUID actorUserId) {
    jdbcTemplate.update(
        """
        update report
        set payment_gate_status = 'UNLOCKED',
            status = case
              when status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED') then 'RELEASED'
              else status
            end,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and status in ('ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED', 'RELEASED')
          and deleted_at is null
        """,
        actorUserId,
        reportId
    );
  }

  private PaymentReceiptRecord record(ResultSet rs) throws SQLException {
    return new PaymentReceiptRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("project_id", UUID.class),
        rs.getObject("report_id", UUID.class),
        rs.getBigDecimal("amount"),
        rs.getTimestamp("received_at").toInstant(),
        rs.getObject("received_by", UUID.class),
        rs.getString("receipt_method"),
        rs.getObject("voucher_file_id", UUID.class),
        rs.getString("note"),
        rs.getString("status")
    );
  }
}
