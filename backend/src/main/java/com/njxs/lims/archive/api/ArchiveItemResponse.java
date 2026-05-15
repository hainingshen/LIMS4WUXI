package com.njxs.lims.archive.api;

import com.njxs.lims.archive.domain.ArchiveItemRecord;
import java.time.Instant;
import java.util.UUID;

public record ArchiveItemResponse(
    UUID id,
    UUID projectId,
    String entityType,
    UUID entityId,
    String archiveType,
    UUID fileId,
    String status,
    Instant archivedAt,
    String projectBizNo,
    String customerOrgName,
    String entityBizNo,
    String fileName,
    String contentType,
    Long sizeBytes
) {

  public static ArchiveItemResponse from(ArchiveItemRecord record) {
    return new ArchiveItemResponse(
        record.id(),
        record.projectId(),
        record.entityType(),
        record.entityId(),
        record.archiveType(),
        record.fileId(),
        record.status(),
        record.archivedAt(),
        record.projectBizNo(),
        record.customerOrgName(),
        record.entityBizNo(),
        record.fileName(),
        record.contentType(),
        record.sizeBytes()
    );
  }
}
