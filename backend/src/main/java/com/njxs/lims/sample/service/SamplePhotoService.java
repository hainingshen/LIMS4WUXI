package com.njxs.lims.sample.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.file.domain.FileMetadata;
import com.njxs.lims.file.persistence.FileMetadataRepository;
import com.njxs.lims.file.storage.ObjectStorage;
import com.njxs.lims.file.storage.StoredFile;
import com.njxs.lims.sample.api.SamplePhotoResponse;
import com.njxs.lims.sample.domain.Sample;
import com.njxs.lims.sample.domain.SamplePhoto;
import com.njxs.lims.sample.persistence.SamplePhotoRepository;
import com.njxs.lims.sample.persistence.SampleRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SamplePhotoService {

  private final SampleRepository sampleRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final SamplePhotoRepository samplePhotoRepository;
  private final ObjectStorage objectStorage;

  public SamplePhotoService(
      SampleRepository sampleRepository,
      FileMetadataRepository fileMetadataRepository,
      SamplePhotoRepository samplePhotoRepository,
      ObjectStorage objectStorage
  ) {
    this.sampleRepository = sampleRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.samplePhotoRepository = samplePhotoRepository;
    this.objectStorage = objectStorage;
  }

  @Transactional
  public SamplePhotoResponse upload(UUID sampleId, MultipartFile file, String note, AuthPrincipal principal) {
    Sample sample = sample(sampleId);
    StoredFile storedFile = objectStorage.store(file, "samples/" + sampleId + "/photos");
    Instant now = Instant.now();

    FileMetadata metadata = new FileMetadata();
    metadata.setId(storedFile.fileId());
    metadata.setFileName(storedFile.fileName());
    metadata.setObjectKey(storedFile.objectKey());
    metadata.setContentType(storedFile.contentType());
    metadata.setSizeBytes(storedFile.sizeBytes());
    metadata.setSha256(storedFile.sha256());
    metadata.setUploadedBy(principal.userId());
    metadata.setUploadedAt(now);
    metadata.setCreatedAt(now);
    metadata.setCreatedBy(principal.userId());
    metadata.setUpdatedAt(now);
    metadata.setUpdatedBy(principal.userId());
    metadata.setTenantOrgId(sample.getTenantOrgId());
    metadata.setSourceType("NEW");
    metadata.setDataQualityScore(100);
    fileMetadataRepository.save(metadata);

    UUID samplePhotoId = UUID.randomUUID();
    samplePhotoRepository.save(new SamplePhoto(
        samplePhotoId,
        sampleId,
        storedFile.fileId(),
        "RECEIVING",
        emptyToNull(note),
        now,
        principal.userId(),
        0
    ));
    return SamplePhotoResponse.from(samplePhotoId, sampleId, storedFile);
  }

  private Sample sample(UUID id) {
    return sampleRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            "NOT_FOUND",
            "资源不存在",
            HttpStatus.NOT_FOUND,
            Map.of("entityType", "sample", "entityId", id)
        ));
  }

  private String emptyToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
