package com.njxs.lims.file.domain;

import java.util.UUID;

public record StoredFile(
    UUID fileId,
    String fileName,
    String objectKey,
    String contentType,
    long sizeBytes,
    String sha256
) {
}
