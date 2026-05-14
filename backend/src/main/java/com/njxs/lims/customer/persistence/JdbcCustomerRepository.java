package com.njxs.lims.customer.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.customer.domain.CustomerContact;
import com.njxs.lims.customer.domain.CustomerOrg;
import com.njxs.lims.customer.domain.Manufacturer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcCustomerRepository implements CustomerRepository {

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
}
