package com.njxs.lims.importexport.domain;

import java.time.Instant;
import java.util.UUID;

public record ImportExportTaskRecord(
    UUID id,
    String bizNo,
    String taskType,
    String entityType,
    String status,
    UUID requestedBy,
    Instant requestedAt,
    UUID sourceFileId,
    UUID resultFileId,
    String errorSummary,
    Instant expiresAt
) {
}
