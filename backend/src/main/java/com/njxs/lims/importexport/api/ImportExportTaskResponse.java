package com.njxs.lims.importexport.api;

import com.njxs.lims.importexport.domain.ImportExportTaskRecord;
import java.time.Instant;
import java.util.UUID;

public record ImportExportTaskResponse(
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

  public static ImportExportTaskResponse from(ImportExportTaskRecord record) {
    return new ImportExportTaskResponse(
        record.id(),
        record.bizNo(),
        record.taskType(),
        record.entityType(),
        record.status(),
        record.requestedBy(),
        record.requestedAt(),
        record.sourceFileId(),
        record.resultFileId(),
        record.errorSummary(),
        record.expiresAt()
    );
  }
}
