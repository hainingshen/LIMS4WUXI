package com.njxs.lims.file.service;

import com.njxs.lims.auth.domain.AuthPrincipal;
import com.njxs.lims.common.exception.BusinessException;
import com.njxs.lims.file.api.FileUploadResponse;
import com.njxs.lims.file.domain.FileMetadata;
import com.njxs.lims.file.persistence.FileMetadataRepository;
import com.njxs.lims.file.storage.ObjectStorage;
import com.njxs.lims.file.storage.StoredFile;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "lims.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileService {

  private static final Set<String> PURPOSES = Set.of(
      "GENERAL",
      "SAMPLE",
      "DEVICE",
      "REPORT",
      "CONTRACT",
      "PAYMENT",
      "ARCHIVE",
      "RAW_RECORD",
      "IMPORT_EXPORT"
  );

  private final ObjectStorage objectStorage;
  private final FileMetadataRepository fileMetadataRepository;

  public FileService(ObjectStorage objectStorage, FileMetadataRepository fileMetadataRepository) {
    this.objectStorage = objectStorage;
    this.fileMetadataRepository = fileMetadataRepository;
  }

  @Transactional
  public FileUploadResponse upload(MultipartFile file, String purpose, AuthPrincipal principal) {
    StoredFile storedFile = objectStorage.store(file, folder(purpose, principal));
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
    metadata.setTenantOrgId(principal.orgId());
    metadata.setSourceType("NEW");
    metadata.setDataQualityScore(100);
    fileMetadataRepository.save(metadata);

    return FileUploadResponse.from(storedFile);
  }

  private String folder(String purpose, AuthPrincipal principal) {
    String normalized = normalizePurpose(purpose);
    return "uploads/" + normalized.toLowerCase(Locale.ROOT).replace("_", "-") + "/" + principal.userId();
  }

  private String normalizePurpose(String value) {
    String normalized = value == null || value.isBlank()
        ? "GENERAL"
        : value.trim().toUpperCase(Locale.ROOT).replace("-", "_");
    if (!PURPOSES.contains(normalized)) {
      throw new BusinessException(
          "FILE_PURPOSE_INVALID",
          "文件用途不合法",
          HttpStatus.BAD_REQUEST,
          Map.of("purpose", String.valueOf(value), "allowed", PURPOSES)
      );
    }
    return normalized;
  }
}
