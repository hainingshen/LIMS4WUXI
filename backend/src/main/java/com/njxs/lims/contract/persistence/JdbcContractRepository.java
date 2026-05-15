package com.njxs.lims.contract.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.contract.domain.Contract;
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
public class JdbcContractRepository implements ContractRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, customer_org_id, contract_type, template_code, status,
      source_file_id, signed_at, signed_file_id, created_at, created_by, updated_at, updated_by,
      deleted_at, version, tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
      """;
  private static final String SELECT_CONTRACT_COLUMNS = "c."
      + SELECT_COLUMNS.strip().replaceAll("\\s+", " ").replace(", ", ", c.");

  private final JdbcTemplate jdbcTemplate;

  public JdbcContractRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextContractBizNo(int year) {
    String prefix = "CON-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from contract where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    int sequence = (count == null ? 0 : count) + 1;
    return prefix + "%04d".formatted(sequence);
  }

  @Override
  public void save(Contract contract) {
    prepareForInsert(contract);
    jdbcTemplate.update(
        """
        insert into contract (
          id, biz_no, project_id, customer_org_id, contract_type, template_code, status,
          source_file_id, signed_at, signed_file_id, created_at, created_by, updated_at, updated_by,
          deleted_at, version, tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        contract.getId(),
        contract.getBizNo(),
        contract.getProjectId(),
        contract.getCustomerOrgId(),
        contract.getContractType(),
        contract.getTemplateCode(),
        contract.getStatus(),
        contract.getSourceFileId(),
        timestamp(contract.getSignedAt()),
        contract.getSignedFileId(),
        timestamp(contract.getCreatedAt()),
        contract.getCreatedBy(),
        timestamp(contract.getUpdatedAt()),
        contract.getUpdatedBy(),
        timestamp(contract.getDeletedAt()),
        contract.getVersion(),
        contract.getTenantOrgId(),
        contract.getLegacyId(),
        contract.getSourceType(),
        contract.getMigrationBatch(),
        contract.getDataQualityScore()
    );
  }

  @Override
  public Optional<Contract> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from contract
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> contract(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public List<Contract> list(int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from contract
        where deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> contract(rs),
        limit,
        offset
    );
  }

  @Override
  public int count() {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract
        where deleted_at is null
        """,
        Integer.class
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<Contract> listForCustomerOrg(UUID customerOrgId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from contract
        where customer_org_id = ?
          and deleted_at is null
        order by updated_at desc, created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> contract(rs),
        customerOrgId,
        limit,
        offset
    );
  }

  @Override
  public int countForCustomerOrg(UUID customerOrgId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract
        where customer_org_id = ?
          and deleted_at is null
        """,
        Integer.class,
        customerOrgId
    );
    return count == null ? 0 : count;
  }

  @Override
  public List<Contract> listForApplicant(UUID applicantUserId, int limit, int offset) {
    return jdbcTemplate.query(
        "select " + SELECT_CONTRACT_COLUMNS + " " + """
        from contract c
        join project p on p.id = c.project_id
        join submission s on s.id = p.submission_id
        where s.applicant_user_id = ?
          and c.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        order by c.updated_at desc, c.created_at desc
        limit ? offset ?
        """,
        (rs, rowNum) -> contract(rs),
        applicantUserId,
        limit,
        offset
    );
  }

  @Override
  public int countForApplicant(UUID applicantUserId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract c
        join project p on p.id = c.project_id
        join submission s on s.id = p.submission_id
        where s.applicant_user_id = ?
          and c.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        """,
        Integer.class,
        applicantUserId
    );
    return count == null ? 0 : count;
  }

  @Override
  public Contract markSigned(Contract contract, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update contract " + """
        set status = ?,
            signed_at = ?,
            signed_file_id = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> contract(rs),
        contract.getStatus(),
        timestamp(contract.getSignedAt()),
        contract.getSignedFileId(),
        actorUserId,
        contract.getId()
    );
  }

  @Override
  public boolean existsByAssignedSales(UUID contractId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract c
        join project p on p.id = c.project_id
        join submission s on s.id = p.submission_id
        where c.id = ?
          and s.assigned_sales_user_id = ?
          and c.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        """,
        Integer.class,
        contractId,
        userId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsByApplicant(UUID contractId, UUID userId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract c
        join project p on p.id = c.project_id
        join submission s on s.id = p.submission_id
        where c.id = ?
          and s.applicant_user_id = ?
          and c.deleted_at is null
          and p.deleted_at is null
          and s.deleted_at is null
        """,
        Integer.class,
        contractId,
        userId
    );
    return count != null && count > 0;
  }

  @Override
  public boolean existsByCustomerOrg(UUID contractId, UUID customerOrgId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from contract c
        where c.id = ?
          and c.customer_org_id = ?
          and c.deleted_at is null
        """,
        Integer.class,
        contractId,
        customerOrgId
    );
    return count != null && count > 0;
  }

  private Contract contract(ResultSet rs) throws SQLException {
    Contract contract = new Contract();
    contract.setId(rs.getObject("id", UUID.class));
    contract.setBizNo(rs.getString("biz_no"));
    contract.setProjectId(rs.getObject("project_id", UUID.class));
    contract.setCustomerOrgId(rs.getObject("customer_org_id", UUID.class));
    contract.setContractType(rs.getString("contract_type"));
    contract.setTemplateCode(rs.getString("template_code"));
    contract.setStatus(rs.getString("status"));
    contract.setSourceFileId(rs.getObject("source_file_id", UUID.class));
    contract.setSignedAt(instant(rs.getTimestamp("signed_at")));
    contract.setSignedFileId(rs.getObject("signed_file_id", UUID.class));
    contract.setCreatedAt(instant(rs.getTimestamp("created_at")));
    contract.setCreatedBy(rs.getObject("created_by", UUID.class));
    contract.setUpdatedAt(instant(rs.getTimestamp("updated_at")));
    contract.setUpdatedBy(rs.getObject("updated_by", UUID.class));
    contract.setDeletedAt(instant(rs.getTimestamp("deleted_at")));
    contract.setVersion((Integer) rs.getObject("version"));
    contract.setTenantOrgId(rs.getObject("tenant_org_id", UUID.class));
    contract.setLegacyId(rs.getString("legacy_id"));
    contract.setSourceType(rs.getString("source_type"));
    contract.setMigrationBatch(rs.getString("migration_batch"));
    contract.setDataQualityScore((Integer) rs.getObject("data_quality_score"));
    return contract;
  }

  private Instant instant(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant();
  }
}
