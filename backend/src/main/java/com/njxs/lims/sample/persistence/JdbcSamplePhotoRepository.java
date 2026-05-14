package com.njxs.lims.sample.persistence;

import com.njxs.lims.sample.domain.SamplePhoto;
import java.sql.Timestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSamplePhotoRepository implements SamplePhotoRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSamplePhotoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(SamplePhoto samplePhoto) {
    jdbcTemplate.update(
        """
        insert into sample_photo (
          id, sample_id, file_id, photo_type, note, uploaded_at, uploaded_by, sort_order
        ) values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        samplePhoto.id(),
        samplePhoto.sampleId(),
        samplePhoto.fileId(),
        samplePhoto.photoType(),
        samplePhoto.note(),
        Timestamp.from(samplePhoto.uploadedAt()),
        samplePhoto.uploadedBy(),
        samplePhoto.sortOrder()
    );
  }
}
