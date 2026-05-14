package com.njxs.lims.file.persistence;

import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.prepareForInsert;
import static com.njxs.lims.common.persistence.BaseEntityJdbcSupport.timestamp;

import com.njxs.lims.file.domain.FileMetadata;
import java.sql.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcFileMetadataRepository implements FileMetadataRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcFileMetadataRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(FileMetadata fileMetadata) {
    prepareForInsert(fileMetadata);
    jdbcTemplate.update(
        """
        insert into file_metadata (
          id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by, uploaded_at,
          retention_until, created_at, created_by, updated_at, updated_by, deleted_at, version,
          tenant_org_id, legacy_id, source_type, migration_batch, data_quality_score
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        fileMetadata.getId(),
        fileMetadata.getFileName(),
        fileMetadata.getObjectKey(),
        fileMetadata.getContentType(),
        fileMetadata.getSizeBytes(),
        fileMetadata.getSha256(),
        fileMetadata.getUploadedBy(),
        timestamp(fileMetadata.getUploadedAt()),
        fileMetadata.getRetentionUntil() == null ? null : Date.valueOf(fileMetadata.getRetentionUntil()),
        timestamp(fileMetadata.getCreatedAt()),
        fileMetadata.getCreatedBy(),
        timestamp(fileMetadata.getUpdatedAt()),
        fileMetadata.getUpdatedBy(),
        timestamp(fileMetadata.getDeletedAt()),
        fileMetadata.getVersion(),
        fileMetadata.getTenantOrgId(),
        fileMetadata.getLegacyId(),
        fileMetadata.getSourceType(),
        fileMetadata.getMigrationBatch(),
        fileMetadata.getDataQualityScore()
    );
  }
}
