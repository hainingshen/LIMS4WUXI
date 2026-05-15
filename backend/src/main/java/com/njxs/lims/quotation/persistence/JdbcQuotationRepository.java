package com.njxs.lims.quotation.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.quotation.domain.Quotation;
import com.njxs.lims.quotation.domain.QuotationItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcQuotationRepository implements QuotationRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, customer_org_id, status, currency, subtotal_amount,
      discount_amount, total_amount, valid_until, note, created_at, created_by, updated_at, updated_by,
      deleted_at, version, tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcQuotationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextQuotationBizNo(int year) {
    String prefix = "Q-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from quotation where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    int sequence = (count == null ? 0 : count) + 1;
    return prefix + "%04d".formatted(sequence);
  }

  @Override
  public void save(Quotation quotation, List<QuotationItem> items) {
    prepareForInsert(quotation);
    jdbcTemplate.update(
        """
        insert into quotation (
          id, biz_no, project_id, customer_org_id, status, currency, subtotal_amount, discount_amount,
          total_amount, valid_until, note, created_at, created_by, updated_at, updated_by, deleted_at,
          version, tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        quotation.getId(),
        quotation.getBizNo(),
        quotation.getProjectId(),
        quotation.getCustomerOrgId(),
        quotation.getStatus(),
        quotation.getCurrency(),
        quotation.getSubtotalAmount(),
        quotation.getDiscountAmount(),
        quotation.getTotalAmount(),
        quotation.getValidUntil(),
        quotation.getNote(),
        timestamp(quotation.getCreatedAt()),
        quotation.getCreatedBy(),
        timestamp(quotation.getUpdatedAt()),
        quotation.getUpdatedBy(),
        timestamp(quotation.getDeletedAt()),
        quotation.getVersion(),
        quotation.getTenantOrgId(),
        quotation.getLegacyId(),
        quotation.getSourceType(),
        quotation.getMigrationBatch(),
        quotation.getDataQualityScore()
    );
    for (QuotationItem item : items) {
      jdbcTemplate.update(
          """
          insert into quotation_item (
            id, quotation_id, test_name, unit_price, quantity, line_amount, sort_order
          ) values (?, ?, ?, ?, ?, ?, ?)
          """,
          item.id(),
          item.quotationId(),
          item.testName(),
          item.unitPrice(),
          item.quantity(),
          item.lineAmount(),
          item.sortOrder()
      );
    }
  }

  @Override
  public List<Quotation> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from quotation
        where deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> quotation(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from quotation
        where deleted_at is null
        """,
        Integer.class
    );
    return count == null ? 0 : count;
  }

  private Quotation quotation(ResultSet rs) throws SQLException {
    Quotation quotation = new Quotation();
    quotation.setId(rs.getObject("id", UUID.class));
    quotation.setBizNo(rs.getString("biz_no"));
    quotation.setProjectId(rs.getObject("project_id", UUID.class));
    quotation.setCustomerOrgId(rs.getObject("customer_org_id", UUID.class));
    quotation.setStatus(rs.getString("status"));
    quotation.setCurrency(rs.getString("currency"));
    quotation.setSubtotalAmount(rs.getBigDecimal("subtotal_amount"));
    quotation.setDiscountAmount(rs.getBigDecimal("discount_amount"));
    quotation.setTotalAmount(rs.getBigDecimal("total_amount"));
    quotation.setValidUntil(rs.getObject("valid_until", java.time.LocalDate.class));
    quotation.setNote(rs.getString("note"));
    quotation.setCreatedAt(instant(rs.getTimestamp("created_at")));
    quotation.setCreatedBy(rs.getObject("created_by", UUID.class));
    quotation.setUpdatedAt(instant(rs.getTimestamp("updated_at")));
    quotation.setUpdatedBy(rs.getObject("updated_by", UUID.class));
    quotation.setDeletedAt(instant(rs.getTimestamp("deleted_at")));
    quotation.setVersion((Integer) rs.getObject("version"));
    quotation.setTenantOrgId(rs.getObject("tenant_org_id", UUID.class));
    quotation.setLegacyId(rs.getString("legacy_id"));
    quotation.setSourceType(rs.getString("source_type"));
    quotation.setMigrationBatch(rs.getString("migration_batch"));
    quotation.setDataQualityScore((Integer) rs.getObject("data_quality_score"));
    return quotation;
  }

  private java.time.Instant instant(java.sql.Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
