package com.njxs.lims.file.api;

import com.njxs.lims.file.storage.StoredFile;
import java.util.UUID;

public record FileUploadResponse(
    UUID fileId,
    String fileName,
    String objectKey,
    String contentType,
    long sizeBytes,
    String sha256
) {

  public static FileUploadResponse from(StoredFile storedFile) {
    return new FileUploadResponse(
        storedFile.fileId(),
        storedFile.fileName(),
        storedFile.objectKey(),
        storedFile.contentType(),
        storedFile.sizeBytes(),
        storedFile.sha256()
    );
  }
}
