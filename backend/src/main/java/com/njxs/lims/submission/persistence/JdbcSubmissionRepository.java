package com.njxs.lims.submission.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSubmissionRepository implements SubmissionRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSubmissionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<SubmissionRecord> findById(UUID id) {
    List<SubmissionRecord> records = jdbcTemplate.query(
        """
        select id, biz_no, customer_org_id, applicant_user_id, status
        from submission
        where id = ?
          and deleted_at is null
        """,
        (rs, rowNum) -> record(rs),
        id
    );
    return records.stream().findFirst();
  }

  @Override
  public boolean existsByApplicant(UUID id, UUID applicantUserId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from submission
        where id = ?
          and applicant_user_id = ?
          and deleted_at is null
        """,
        Integer.class,
        id,
        applicantUserId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsByCustomerOrg(UUID id, UUID customerOrgId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from submission
        where id = ?
          and customer_org_id = ?
          and deleted_at is null
        """,
        Integer.class,
        id,
        customerOrgId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsByAssignedSales(UUID id, UUID salesUserId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from submission
        where id = ?
          and assigned_sales_user_id = ?
          and deleted_at is null
        """,
        Integer.class,
        id,
        salesUserId
    );
    return count != null && count > 0;
  }

  @Override
  public SubmissionRecord markSubmitted(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        """
        update submission
        set status = 'SUBMITTED',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning id, biz_no, customer_org_id, applicant_user_id, status
        """,
        (rs, rowNum) -> record(rs),
        actorUserId,
        id
    );
  }

  @Override
  public SubmissionRecord markAccepted(UUID id, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        """
        update submission
        set status = 'ACCEPTED',
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning id, biz_no, customer_org_id, applicant_user_id, status
        """,
        (rs, rowNum) -> record(rs),
        actorUserId,
        id
    );
  }

  private SubmissionRecord record(ResultSet rs) throws SQLException {
    return new SubmissionRecord(
        rs.getObject("id", UUID.class),
        rs.getString("biz_no"),
        rs.getObject("customer_org_id", UUID.class),
        rs.getObject("applicant_user_id", UUID.class),
        rs.getString("status")
    );
  }
}
