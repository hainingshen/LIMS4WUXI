package com.njxs.lims.customer.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.customer.domain.CustomerContact;
import com.njxs.lims.customer.domain.CustomerOrg;
import com.njxs.lims.customer.domain.CustomerOrgRecord;
import com.njxs.lims.customer.domain.Manufacturer;
import com.njxs.lims.customer.domain.PortalUserRecord;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcCustomerRepository implements CustomerRepository {

  private static final String CUSTOMER_ORG_COLUMNS = """
      id, name_zh, name_en, address_zh, address_en, tax_no, invoice_title, status
      """;
  private static final String PORTAL_USER_COLUMNS = """
      au.id, au.customer_org_id, au.username, au.display_name, au.email,
      coalesce(array_agg(ur.role_code order by ur.role_code) filter (where ur.role_code is not null), '{}') role_codes,
      au.status
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcCustomerRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void saveCustomerOrg(CustomerOrg customerOrg) {
    prepareForInsert(customerOrg);
    jdbcTemplate.update(
        """
        insert into customer_org (
          id, biz_no, name_zh, name_en, address_zh, address_en, industry, grade, tax_no,
          invoice_title, bank_account, status, created_at, created_by, updated_at, updated_by,
          deleted_at, version, tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        customerOrg.getId(),
        customerOrg.getBizNo(),
        customerOrg.getNameZh(),
        customerOrg.getNameEn(),
        customerOrg.getAddressZh(),
        customerOrg.getAddressEn(),
        customerOrg.getIndustry(),
        customerOrg.getGrade(),
        customerOrg.getTaxNo(),
        customerOrg.getInvoiceTitle(),
        customerOrg.getBankAccount(),
        customerOrg.getStatus(),
        timestamp(customerOrg.getCreatedAt()),
        customerOrg.getCreatedBy(),
        timestamp(customerOrg.getUpdatedAt()),
        customerOrg.getUpdatedBy(),
        timestamp(customerOrg.getDeletedAt()),
        customerOrg.getVersion(),
        customerOrg.getTenantOrgId(),
        customerOrg.getLegacyId(),
        customerOrg.getSourceType(),
        customerOrg.getMigrationBatch(),
        customerOrg.getDataQualityScore()
    );
  }

  @Override
  public void saveCustomerContact(CustomerContact customerContact) {
    prepareForInsert(customerContact);
    jdbcTemplate.update(
        """
        insert into customer_contact (
          id, biz_no, customer_org_id, contact_name, phone, email, is_primary, status,
          created_at, created_by, updated_at, updated_by, deleted_at, version, tenant_org_id,
          legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        customerContact.getId(),
        customerContact.getBizNo(),
        customerContact.getCustomerOrgId(),
        customerContact.getContactName(),
        customerContact.getPhone(),
        customerContact.getEmail(),
        customerContact.isPrimary(),
        customerContact.getStatus(),
        timestamp(customerContact.getCreatedAt()),
        customerContact.getCreatedBy(),
        timestamp(customerContact.getUpdatedAt()),
        customerContact.getUpdatedBy(),
        timestamp(customerContact.getDeletedAt()),
        customerContact.getVersion(),
        customerContact.getTenantOrgId(),
        customerContact.getLegacyId(),
        customerContact.getSourceType(),
        customerContact.getMigrationBatch(),
        customerContact.getDataQualityScore()
    );
  }

  @Override
  public void saveManufacturer(Manufacturer manufacturer) {
    prepareForInsert(manufacturer);
    jdbcTemplate.update(
        """
        insert into manufacturer (
          id, biz_no, name_zh, name_en, address_zh, address_en, status, created_at, created_by,
          updated_at, updated_by, deleted_at, version, tenant_org_id, legacy_id, source_type,
          migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        manufacturer.getId(),
        manufacturer.getBizNo(),
        manufacturer.getNameZh(),
        manufacturer.getNameEn(),
        manufacturer.getAddressZh(),
        manufacturer.getAddressEn(),
        manufacturer.getStatus(),
        timestamp(manufacturer.getCreatedAt()),
        manufacturer.getCreatedBy(),
        timestamp(manufacturer.getUpdatedAt()),
        manufacturer.getUpdatedBy(),
        timestamp(manufacturer.getDeletedAt()),
        manufacturer.getVersion(),
        manufacturer.getTenantOrgId(),
        manufacturer.getLegacyId(),
        manufacturer.getSourceType(),
        manufacturer.getMigrationBatch(),
        manufacturer.getDataQualityScore()
    );
  }

  @Override
  public List<CustomerOrgRecord> listCustomerOrgs(String keyword, int limit, int offset) {
    if (keyword == null) {
      return jdbcTemplate.query(
          "select " + CUSTOMER_ORG_COLUMNS + """
          from customer_org
          where deleted_at is null
          order by name_zh, id
          limit ? offset ?
          """,
          (rs, rowNum) -> customerOrg(rs),
          limit,
          offset
      );
    }
    String pattern = "%" + keyword.toLowerCase() + "%";
    return jdbcTemplate.query(
        "select " + CUSTOMER_ORG_COLUMNS + """
        from customer_org
        where deleted_at is null
          and (
            lower(name_zh) like ?
            or lower(coalesce(name_en, '')) like ?
            or lower(coalesce(tax_no, '')) like ?
            or lower(coalesce(invoice_title, '')) like ?
          )
        order by name_zh, id
        limit ? offset ?
        """,
        (rs, rowNum) -> customerOrg(rs),
        pattern,
        pattern,
        pattern,
        pattern,
        limit,
        offset
    );
  }

  @Override
  public int countCustomerOrgs(String keyword) {
    Integer count;
    if (keyword == null) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from customer_org where deleted_at is null",
          Integer.class
      );
    } else {
      String pattern = "%" + keyword.toLowerCase() + "%";
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from customer_org
          where deleted_at is null
            and (
              lower(name_zh) like ?
              or lower(coalesce(name_en, '')) like ?
              or lower(coalesce(tax_no, '')) like ?
              or lower(coalesce(invoice_title, '')) like ?
            )
          """,
          Integer.class,
          pattern,
          pattern,
          pattern,
          pattern
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public boolean existsCustomerOrg(UUID id) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from customer_org where id = ? and deleted_at is null",
        Integer.class,
        id
    );
    return count != null && count > 0;
  }

  @Override
  public Optional<String> customerOrgStatus(UUID id) {
    return jdbcTemplate.query(
        "select status from customer_org where id = ? and deleted_at is null",
        (rs, rowNum) -> rs.getString("status"),
        id
    ).stream().findFirst();
  }

  @Override
  public String nextCustomerOrgBizNo(int year) {
    String prefix = "CUST-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from customer_org where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    return prefix + "%05d".formatted((count == null ? 0 : count) + 1);
  }

  @Override
  public CustomerOrgRecord insertCustomerOrg(CustomerOrgRecord record, String bizNo, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "insert into customer_org " + """
        (
          id, biz_no, name_zh, name_en, address_zh, address_en, tax_no, invoice_title, status,
          created_at, created_by, updated_at, updated_by, version, tenant_org_id, source_type,
          data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?, 0, ?, 'NEW', 100)
        returning
        """ + CUSTOMER_ORG_COLUMNS,
        (rs, rowNum) -> customerOrg(rs),
        record.id(),
        bizNo,
        record.nameZh(),
        record.nameEn(),
        record.addressZh(),
        record.addressEn(),
        record.taxNo(),
        record.invoiceTitle(),
        record.status(),
        actorUserId,
        actorUserId,
        record.id()
    );
  }

  @Override
  public CustomerOrgRecord updateCustomerOrg(CustomerOrgRecord record, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update customer_org " + """
        set name_zh = ?,
            name_en = ?,
            address_zh = ?,
            address_en = ?,
            tax_no = ?,
            invoice_title = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ?
          and deleted_at is null
        returning
        """ + CUSTOMER_ORG_COLUMNS,
        (rs, rowNum) -> customerOrg(rs),
        record.nameZh(),
        record.nameEn(),
        record.addressZh(),
        record.addressEn(),
        record.taxNo(),
        record.invoiceTitle(),
        actorUserId,
        record.id()
    );
  }

  @Override
  public List<PortalUserRecord> listPortalUsers(UUID customerOrgId, int limit, int offset) {
    if (customerOrgId == null) {
      return jdbcTemplate.query(
          "select " + PORTAL_USER_COLUMNS + """
          from app_user au
          left join user_role ur on ur.user_id = au.id
          where au.client_type = 'PORTAL'
            and au.deleted_at is null
          group by au.id
          order by au.created_at desc, au.username
          limit ? offset ?
          """,
          (rs, rowNum) -> portalUser(rs),
          limit,
          offset
      );
    }
    return jdbcTemplate.query(
        "select " + PORTAL_USER_COLUMNS + """
        from app_user au
        left join user_role ur on ur.user_id = au.id
        where au.client_type = 'PORTAL'
          and au.customer_org_id = ?
          and au.deleted_at is null
        group by au.id
        order by au.created_at desc, au.username
        limit ? offset ?
        """,
        (rs, rowNum) -> portalUser(rs),
        customerOrgId,
        limit,
        offset
    );
  }

  @Override
  public int countPortalUsers(UUID customerOrgId) {
    Integer count;
    if (customerOrgId == null) {
      count = jdbcTemplate.queryForObject(
          "select count(*) from app_user where client_type = 'PORTAL' and deleted_at is null",
          Integer.class
      );
    } else {
      count = jdbcTemplate.queryForObject(
          """
          select count(*)
          from app_user
          where client_type = 'PORTAL'
            and customer_org_id = ?
            and deleted_at is null
          """,
          Integer.class,
          customerOrgId
      );
    }
    return count == null ? 0 : count;
  }

  @Override
  public boolean existsUsername(String username) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from app_user where username = ? and deleted_at is null",
        Integer.class,
        username
    );
    return count != null && count > 0;
  }

  @Override
  public PortalUserRecord insertPortalUser(PortalUserRecord record, String passwordHash, UUID actorUserId) {
    jdbcTemplate.update(
        """
        insert into app_user (
          id, username, display_name, password_hash, client_type, customer_org_id, email, status,
          created_at, created_by, updated_at, updated_by, version, tenant_org_id, source_type,
          data_quality_score
        ) values (?, ?, ?, ?, 'PORTAL', ?, ?, ?, now(), ?, now(), ?, 0, ?, 'NEW', 100)
        """,
        record.id(),
        record.username(),
        record.displayName(),
        passwordHash,
        record.customerOrgId(),
        record.email(),
        record.status(),
        actorUserId,
        actorUserId,
        record.customerOrgId()
    );
    for (String roleCode : record.roleCodes()) {
      jdbcTemplate.update(
          "insert into user_role (user_id, role_code) values (?, ?) on conflict do nothing",
          record.id(),
          roleCode
      );
    }
    return findPortalUser(record.id()).orElse(record);
  }

  private Optional<PortalUserRecord> findPortalUser(UUID id) {
    return jdbcTemplate.query(
        "select " + PORTAL_USER_COLUMNS + """
        from app_user au
        left join user_role ur on ur.user_id = au.id
        where au.id = ?
          and au.client_type = 'PORTAL'
          and au.deleted_at is null
        group by au.id
        """,
        (rs, rowNum) -> portalUser(rs),
        id
    ).stream().findFirst();
  }

  private CustomerOrgRecord customerOrg(ResultSet rs) throws SQLException {
    return new CustomerOrgRecord(
        rs.getObject("id", UUID.class),
        rs.getString("name_zh"),
        rs.getString("name_en"),
        rs.getString("address_zh"),
        rs.getString("address_en"),
        rs.getString("tax_no"),
        rs.getString("invoice_title"),
        rs.getString("status")
    );
  }

  private PortalUserRecord portalUser(ResultSet rs) throws SQLException {
    return new PortalUserRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("customer_org_id", UUID.class),
        rs.getString("username"),
        rs.getString("display_name"),
        rs.getString("email"),
        roleCodes(rs.getArray("role_codes")),
        rs.getString("status")
    );
  }

  private List<String> roleCodes(Array sqlArray) throws SQLException {
    if (sqlArray == null) {
      return List.of();
    }
    Object array = sqlArray.getArray();
    if (array instanceof String[] values) {
      return List.of(values);
    }
    if (array instanceof Object[] values) {
      return Arrays.stream(values).map(String::valueOf).toList();
    }
    return List.of();
  }
}
