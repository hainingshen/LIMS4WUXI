package com.njxs.lims.sample.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.sample.domain.Sample;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSampleRepository implements SampleRepository {

  private static final String SELECT_COLUMNS = """
      id, biz_no, project_id, submission_id, sample_name_zh, sample_name_en, sterilization_state,
      cas_code, model_ref, size_spec, lot_batch, raw_material, packaging_material, physical_state,
      color, density, stability, solubility, storage_condition, intended_use, additional_info,
      quantity, quantity_unit, status, received_at, received_by, recipient_name, received_date,
      inventory_location, created_at, created_by, updated_at, updated_by, deleted_at, version,
      tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcSampleRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String nextSampleBizNo(int year) {
    String prefix = "S-" + year + "-";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from sample where biz_no like ?",
        Integer.class,
        prefix + "%"
    );
    int sequence = (count == null ? 0 : count) + 1;
    return prefix + "%04d".formatted(sequence);
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

  @Override
  public Optional<Sample> findById(UUID id) {
    return jdbcTemplate.query(
        "select " + SELECT_COLUMNS + """
        from sample
        where id = ? and deleted_at is null
        """,
        (rs, rowNum) -> sample(rs),
        id
    ).stream().findFirst();
  }

  @Override
  public Sample updateWorkflow(Sample sample, UUID actorUserId) {
    return jdbcTemplate.queryForObject(
        "update sample " + """
        set status = ?,
            received_at = ?,
            received_by = ?,
            recipient_name = ?,
            received_date = ?,
            inventory_location = ?,
            updated_at = now(),
            updated_by = ?,
            version = version + 1
        where id = ? and deleted_at is null
        returning
        """ + SELECT_COLUMNS,
        (rs, rowNum) -> sample(rs),
        sample.getStatus(),
        timestamp(sample.getReceivedAt()),
        sample.getReceivedBy(),
        sample.getRecipientName(),
        sample.getReceivedDate() == null ? null : Date.valueOf(sample.getReceivedDate()),
        sample.getInventoryLocation(),
        actorUserId,
        sample.getId()
    );
  }

  @Override
  public boolean allProjectSamplesInStock(UUID projectId) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(*)
        from sample
        where project_id = ?
          and deleted_at is null
          and status not in (
            'IN_STOCK', 'CONDITIONING', 'READY_FOR_TEST', 'IN_TESTING', 'TEST_COMPLETED',
            'PENDING_RETURN', 'PENDING_DISPOSAL', 'RETURNED', 'DISPOSED', 'ARCHIVED'
          )
        """,
        Integer.class,
        projectId
    );
    return count != null && count == 0;
  }

  private Sample sample(ResultSet rs) throws SQLException {
    Sample sample = new Sample();
    sample.setId(rs.getObject("id", UUID.class));
    sample.setBizNo(rs.getString("biz_no"));
    sample.setProjectId(rs.getObject("project_id", UUID.class));
    sample.setSubmissionId(rs.getObject("submission_id", UUID.class));
    sample.setSampleNameZh(rs.getString("sample_name_zh"));
    sample.setSampleNameEn(rs.getString("sample_name_en"));
    sample.setSterilizationState(rs.getString("sterilization_state"));
    sample.setCasCode(rs.getString("cas_code"));
    sample.setModelRef(rs.getString("model_ref"));
    sample.setSizeSpec(rs.getString("size_spec"));
    sample.setLotBatch(rs.getString("lot_batch"));
    sample.setRawMaterial(rs.getString("raw_material"));
    sample.setPackagingMaterial(rs.getString("packaging_material"));
    sample.setPhysicalState(rs.getString("physical_state"));
    sample.setColor(rs.getString("color"));
    sample.setDensity(rs.getString("density"));
    sample.setStability(rs.getString("stability"));
    sample.setSolubility(rs.getString("solubility"));
    sample.setStorageCondition(rs.getString("storage_condition"));
    sample.setIntendedUse(rs.getString("intended_use"));
    sample.setAdditionalInfo(rs.getString("additional_info"));
    sample.setQuantity(rs.getBigDecimal("quantity"));
    sample.setQuantityUnit(rs.getString("quantity_unit"));
    sample.setStatus(rs.getString("status"));
    sample.setReceivedAt(instant(rs.getTimestamp("received_at")));
    sample.setReceivedBy(rs.getObject("received_by", UUID.class));
    sample.setRecipientName(rs.getString("recipient_name"));
    Date receivedDate = rs.getDate("received_date");
    sample.setReceivedDate(receivedDate == null ? null : receivedDate.toLocalDate());
    sample.setInventoryLocation(rs.getString("inventory_location"));
    sample.setCreatedAt(instant(rs.getTimestamp("created_at")));
    sample.setCreatedBy(rs.getObject("created_by", UUID.class));
    sample.setUpdatedAt(instant(rs.getTimestamp("updated_at")));
    sample.setUpdatedBy(rs.getObject("updated_by", UUID.class));
    sample.setDeletedAt(instant(rs.getTimestamp("deleted_at")));
    sample.setVersion((Integer) rs.getObject("version"));
    sample.setTenantOrgId(rs.getObject("tenant_org_id", UUID.class));
    sample.setLegacyId(rs.getString("legacy_id"));
    sample.setSourceType(rs.getString("source_type"));
    sample.setMigrationBatch(rs.getString("migration_batch"));
    sample.setDataQualityScore((Integer) rs.getObject("data_quality_score"));
    return sample;
  }

  private Instant instant(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant();
  }
}
