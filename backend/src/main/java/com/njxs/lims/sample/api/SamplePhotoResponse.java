package com.njxs.lims.sample.api;

import com.njxs.lims.file.storage.StoredFile;
import java.util.UUID;

public record SamplePhotoResponse(
    UUID id,
    UUID sampleId,
    UUID fileId,
    String fileName,
    String objectKey,
    String contentType,
    String sha256,
    long sizeBytes
) {

  public static SamplePhotoResponse from(UUID id, UUID sampleId, StoredFile storedFile) {
    return new SamplePhotoResponse(
        id,
        sampleId,
        storedFile.fileId(),
        storedFile.fileName(),
        storedFile.objectKey(),
        storedFile.contentType(),
        storedFile.sha256(),
        storedFile.sizeBytes()
    );
  }
}
