package com.njxs.lims.file.storage;

import java.util.UUID;

public record StoredFile(
    UUID fileId,
    String fileName,
    String objectKey,
    String contentType,
    String sha256,
    long sizeBytes
) {
}
