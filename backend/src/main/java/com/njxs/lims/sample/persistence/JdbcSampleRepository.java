package com.njxs.lims.sample.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.sample.domain.Sample;
import java.sql.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSampleRepository implements SampleRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSampleRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void saveSample(Sample sample) {
    prepareForInsert(sample);
    jdbcTemplate.update(
        """
        insert into sample (
          id, biz_no, project_id, submission_id, sample_name_zh, sample_name_en, sterilization_state,
          cas_code, model_ref, size_spec, lot_batch, raw_material, packaging_material, physical_state,
          color, density, stability, solubility, storage_condition, intended_use, additional_info,
          quantity, quantity_unit, status, received_at, received_by, recipient_name, received_date,
          inventory_location, created_at, created_by, updated_at, updated_by, deleted_at, version,
          tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (
          ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        )
        """,
        sample.getId(),
        sample.getBizNo(),
        sample.getProjectId(),
        sample.getSubmissionId(),
        sample.getSampleNameZh(),
        sample.getSampleNameEn(),
        sample.getSterilizationState(),
        sample.getCasCode(),
        sample.getModelRef(),
        sample.getSizeSpec(),
        sample.getLotBatch(),
        sample.getRawMaterial(),
        sample.getPackagingMaterial(),
        sample.getPhysicalState(),
        sample.getColor(),
        sample.getDensity(),
        sample.getStability(),
        sample.getSolubility(),
        sample.getStorageCondition(),
        sample.getIntendedUse(),
        sample.getAdditionalInfo(),
        sample.getQuantity(),
        sample.getQuantityUnit(),
        sample.getStatus(),
        timestamp(sample.getReceivedAt()),
        sample.getReceivedBy(),
        sample.getRecipientName(),
        sample.getReceivedDate() == null ? null : Date.valueOf(sample.getReceivedDate()),
        sample.getInventoryLocation(),
        timestamp(sample.getCreatedAt()),
        sample.getCreatedBy(),
        timestamp(sample.getUpdatedAt()),
        sample.getUpdatedBy(),
        timestamp(sample.getDeletedAt()),
        sample.getVersion(),
        sample.getTenantOrgId(),
        sample.getLegacyId(),
        sample.getSourceType(),
        sample.getMigrationBatch(),
        sample.getDataQualityScore()
    );
  }
}
